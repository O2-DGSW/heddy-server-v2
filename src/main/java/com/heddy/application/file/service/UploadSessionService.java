package com.heddy.application.file.service;

import com.heddy.domain.file.exception.FileError;
import com.heddy.domain.file.exception.FileException;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StorageObject;
import com.heddy.domain.file.model.StoredFile;
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
 * 선언과 다른 파일이 READY 로 지정되는 일은 여기서 막는다.
 */
@Service
@Transactional(readOnly = true)
public class UploadSessionService implements PresignUploadUseCase, CompleteUploadUseCase {

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
        // StoredFile.pending 이 purpose 별 허용 형식·최대 크기를 검증한다. 통과하지 못하면
        // 세션 행을 만들지 않는다.
        StoredFile pending = StoredFile.pending(
                command.userId(),
                command.purpose(),
                ObjectKeyGenerator.generate(command.purpose(), command.userId(), command.contentType()),
                command.contentType(),
                command.fileSize(),
                Instant.now().plus(sessionTtl));
        StoredFile saved = fileRepositoryPort.insert(pending);
        return new PresignUploadResult(
                saved.uploadId(), saved.fileId(), fileStoragePort.createUploadUrl(saved), saved.expiresAt());
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
        if (!file.userId().equals(command.userId())) {
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
        // 최종 문턱은 도메인이 지난다. markReady 가 실측값으로 허용 형식·최대 크기를 재검증한다.
        StoredFile ready = pending.markReady(object);
        return CompleteUploadResult.from(fileRepositoryPort.transition(ready, FileStatus.PENDING));
    }
}
