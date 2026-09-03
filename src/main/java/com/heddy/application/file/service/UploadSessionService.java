package com.heddy.application.file.service;

import com.heddy.domain.file.exception.FileError;
import com.heddy.domain.file.exception.FileException;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StorageObject;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.in.CancelUploadCommand;
import com.heddy.domain.file.port.in.CancelUploadUseCase;
import com.heddy.domain.file.port.in.CompleteUploadCommand;
import com.heddy.domain.file.port.in.CompleteUploadResult;
import com.heddy.domain.file.port.in.CompleteUploadUseCase;
import com.heddy.domain.file.port.in.PresignUploadCommand;
import com.heddy.domain.file.port.in.PresignUploadResult;
import com.heddy.domain.file.port.in.PresignUploadUseCase;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import com.heddy.domain.file.service.ObjectKeyGenerator;
import com.heddy.global.error.ApplicationException;
import com.heddy.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * 업로드 세션 발급과 완료. 검증 기준은 두 단계로 나뉜다.
 *
 * <p>발급(presign)에서는 클라이언트가 <em>선언한</em> 크기와 Content-Type 만으로 purpose 제약을
 * 검증한다. 이 단계의 목적은 명백히 규격 밖의 요청에 URL 을 안겨주지 않는 것이고, 선언이 사실이라는
 * 보장은 아니다.
 *
 * <p>완료(complete)에서는 스토리지 HEAD 로 알아낸 <em>실측</em> 객체 존재·크기·Content-Type 으로
 * 재검증한다. presigned PUT 은 Content-Type 을 서명에 넣지만 크기는 넣지 않으므로, 실물이 없거나
 * 선언과 다른 파일이 READY 로 지정되는 일은 여기서 막는다. 크기는 "선언과 일치"까지 요구한다 —
 * purpose 최대치 이하만으로는 부족하다.
 */
@Service
@Transactional(readOnly = true)
public class UploadSessionService implements PresignUploadUseCase, CompleteUploadUseCase,
        CancelUploadUseCase {

    private final FileRepositoryPort fileRepositoryPort;
    private final FileStoragePort fileStoragePort;
    private final Duration sessionTtl;

    public UploadSessionService(
            FileRepositoryPort fileRepositoryPort,
            FileStoragePort fileStoragePort,
            @Value("${app.storage.upload-session-ttl-seconds}") long sessionTtlSeconds
    ) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.fileStoragePort = fileStoragePort;
        this.sessionTtl = Duration.ofSeconds(sessionTtlSeconds);
    }

    @Override
    @Transactional
    public PresignUploadResult presign(PresignUploadCommand command) {
        // 내부 생성물 용도는 외부 발급 경로가 열어주면 안 된다. 사용자가 ANALYSIS_OVERLAY_INTERNAL
        // 로 객체를 올릴 수 있으면 이후 단계가 그것을 "시스템이 만든 파일"로 신뢰할 근거가 사라진다.
        if (!command.purpose().isExternallyRequestable()) {
            throw new FileException(FileError.PURPOSE_NOT_ALLOWED);
        }
        // StoredFile.pending 이 purpose 별 허용 형식·최대 크기를 검증한다. 통과하지 못하면
        // 세션 행을 만들지 않는다.
        StoredFile pending = StoredFile.pending(
                command.userId(),
                command.purpose(),
                ObjectKeyGenerator.generate(command.purpose(), command.userId(), command.contentType()),
                command.contentType(),
                command.fileName(),
                command.fileSize(),
                command.sha256(),
                Instant.now().plus(sessionTtl));
        StoredFile saved = fileRepositoryPort.insert(pending);
        return new PresignUploadResult(
                saved.uploadId(), saved.fileId(), fileStoragePort.createUploadUrl(saved),
                saved.expiresAt());
    }

    /**
     * 이미 {@code READY} 인 세션의 재완료는 저장된 결과를 다시 돌려주는 멱등 동작으로 정한다.
     *
     * <p>근거 — complete 을 호출한 뒤 응답을 받기 전에 연결이 끊긴 클라이언트는 같은 요청을
     * 재시도한다. 이때 거부(409)로 답하면 실제로는 검증을 통과해 READY 가 된 정상 완료 건이
     * 실패로 기록된다. 반대로 READY 는 종착 상태라 저장된 메타데이터가 바뀔 수 없고(DELETED 로의
     * 전이만 남는다), 그 값을 그대로 돌려줘도 "검증 안 된 파일이 참조되는" 문제가 생기지 않는다.
     * 그래서 재요청에는 HEAD 를 다시 하지 않고 저장된 결과로 답하고, DELETED 세션에 대한
     * 재완료만 FILE_INVALID_STATE 로 거부한다.
     */
    @Override
    @Transactional
    public CompleteUploadResult complete(CompleteUploadCommand command) {
        // 존재 확인보다 소유자 확인이 먼저다. 순서가 바뀌면 남의 uploadId 로 세션 존재를 훑을 수 있다.
        StoredFile file = fileRepositoryPort.findByUploadId(command.uploadId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!command.userId().equals(file.userId())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN_RESOURCE);
        }
        return switch (file.status()) {
            case READY -> CompleteUploadResult.from(file);
            case DELETED -> throw new FileException(FileError.INVALID_STATE_TRANSITION);
            case PENDING -> completePending(file);
        };
    }

    private CompleteUploadResult completePending(StoredFile pending) {
        if (pending.isExpiredAt(Instant.now())) {
            // 만료 세션은 새로 발급받아야 한다. 오래된 PENDING 행은 정리 작업이 회수한다.
            throw new FileException(FileError.UPLOAD_EXPIRED);
        }
        StorageObject object = fileStoragePort.findObject(pending.objectKey())
                .orElseThrow(() -> new FileException(FileError.OBJECT_NOT_FOUND));
        if (object.byteSize() <= 0) {
            // 내용이 없는 객체는 업로드가 끝나지 않은 것과 같다.
            throw new FileException(FileError.OBJECT_NOT_FOUND);
        }
        if (!pending.contentType().equals(object.contentType())) {
            // presigned PUT 이 Content-Type 을 서명에 포함하므로, 스토리지에 기록된 타입이
            // 세션과 다르다는 것은 세션 밖의 경로로 객체가 들어갔다는 뜻이다.
            throw new FileException(FileError.CONTENT_TYPE_MISMATCH);
        }
        if (pending.fileSize() != object.byteSize()) {
            // presigned PUT 은 크기를 서명에 포함하지 않으므로 선언과 다른 크기가 올라올 수 있다.
            // 명세의 "크기 일치" 검증이다. 통과시키면 선언 근거가 없는 객체를 READY 로 확정하고
            // 다른 도메인이 그 메타데이터를 근거로 참조하게 된다.
            throw new FileException(FileError.SIZE_MISMATCH);
        }
        // 최종 문턱은 도메인이 지난다. markReady 가 실측값으로 허용 형식·최대 크기를 재검증한다.
        StoredFile ready = pending.markReady(object);
        return CompleteUploadResult.from(fileRepositoryPort.transition(ready, FileStatus.PENDING));
    }

    /**
     * 취소 규칙 — PENDING 만 실제로 취소하고, DELETED 는 멱등 성공으로 답한다.
     *
     * <p>PENDING 은 사용자가 업로드를 그만둔 상태다. 사용자 사진이라 스토리지에 두면 저장 비용과
     * 개인정보 보관 리스크가 남으므로 객체를 지우고 행을 DELETED 로 전이한다. 세션 만료 여부는
     * 보지 않는다 — 만료된 세션일수록 이미 버려진 업로드라 취소 대상에 더 적합하다.
     *
     * <p>READY 는 거부한다(FILE_INVALID_STATE). READY 는 완료 검증을 통과해 다른 도메인이
     * 참조할 수 있는 파일이고, 그 삭제는 업로드 취소가 아니라 파일 삭제 기능의 몫이다. 반대로
     * 이미 DELETED 인 세션에 대한 재요청은 DELETE 의 멱등성에 따라 성공으로 답한다 — 취소 응답을
     * 잃고 재시도한 클라이언트까지 거부하면 이미 끝난 취소가 실패로 기록된다.
     *
     * <p>여기서 지운 객체는 최종 회수가 아니다. 이미 발급된 presigned PUT URL 은 세션 만료까지
     * 유효해서, 취소와 겹쳐 전송 중이던 PUT 이나 클라이언트 재시도가 객체를 되살릴 수 있다.
     * 그래서 {@code reclaimed_at} 을 채우지 않고 남겨, 만료 이후
     * {@link com.heddy.domain.file.port.in.ReclaimUploadObjectsUseCase} 가 같은 키를 한 번 더
     * 지우고 그때 회수를 확정한다.
     */
    @Override
    @Transactional
    public void cancel(CancelUploadCommand command) {
        // complete 과 같은 순서다. 존재를 먼저 알려주면 남의 uploadId 로 세션을 훑을 수 있다.
        StoredFile file = fileRepositoryPort.findByUploadId(command.uploadId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!command.userId().equals(file.userId())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN_RESOURCE);
        }
        switch (file.status()) {
            case PENDING -> cancelPending(file);
            case READY -> throw new FileException(FileError.INVALID_STATE_TRANSITION);
            case DELETED -> {
                // 정리는 이미 끝났다. 다시 지울 것이 없어도 멱등하게 성공이다.
            }
        }
    }

    private void cancelPending(StoredFile pending) {
        // 행을 먼저 선점한다. 기대 상태를 PENDING 으로 건 조건부 갱신이라 같은 행을 노리는
        // complete 의 PENDING → READY 전이와 DB 에서 직렬화된다. 순서가 반대면(객체 삭제 → 행 전이)
        // complete 가 먼저 READY 로 이겼을 때 이 전이만 0 행으로 실패하고, 트랜잭션 롤백은 이미
        // 끝난 스토리지 삭제를 되돌리지 못해 "READY 행 + 없는 객체"로 끝난다. 선점에 실패하면
        // 아무것도 지우지 않고 FILE_CONCURRENT_MODIFICATION 으로 올라간다.
        StoredFile deleted = fileRepositoryPort.transition(pending.markDeleted(), FileStatus.PENDING);
        // 선점 뒤 삭제가 실패하면 예외가 트랜잭션을 되돌려 행이 PENDING 으로 남고 재시도할 수 있다.
        // 되돌리지 못하고 DELETED 로 커밋된 경우에도 reclaimed_at 이 비어 있으므로, 만료 이후
        // 회수 경로(ReclaimUploadObjectsUseCase)가 같은 키를 다시 지운다.
        fileStoragePort.deleteObject(deleted.objectKey());
    }
}
