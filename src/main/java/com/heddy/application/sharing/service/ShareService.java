package com.heddy.application.sharing.service;

import com.heddy.domain.account.port.out.SecureTokenGeneratorPort;
import com.heddy.domain.account.port.out.TokenHasherPort;
import com.heddy.domain.sharing.model.Share;
import com.heddy.domain.sharing.model.ShareFieldType;
import com.heddy.domain.sharing.model.SharePage;
import com.heddy.domain.sharing.port.in.CreateShareUseCase;
import com.heddy.domain.sharing.port.in.DeleteShareUseCase;
import com.heddy.domain.sharing.port.in.GetShareUseCase;
import com.heddy.domain.sharing.port.in.ListSharesUseCase;
import com.heddy.domain.sharing.port.in.UpdateShareUseCase;
import com.heddy.domain.sharing.port.out.ShareRepositoryPort;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import com.heddy.global.error.ApplicationException;
import com.heddy.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 공유 생성·목록·상세·수정·철회. 생성 때는 대상 소유권을 검증하고(#31 컨벤션) 토큰을 발급해
 * 해시로만 저장한다 — 원문은 201 응답의 share_url 로 딱 한 번 세상에 나온다.
 *
 * <p>소유권 검증은 포트 질의에 소유자 조건을 실어 보내는 방식이다. 남의 기록·공유는 없는 것과
 * 같은 404 이고, 질의 횟수도 같아야 존재 여부가 새지 않는다.
 */
@Service
@Transactional(readOnly = true)
public class ShareService implements CreateShareUseCase, ListSharesUseCase,
        GetShareUseCase, UpdateShareUseCase, DeleteShareUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final ShareRepositoryPort shareRepositoryPort;
    private final TreatmentRecordRepositoryPort treatmentRecordRepositoryPort;
    private final SecureTokenGeneratorPort tokenGeneratorPort;
    private final TokenHasherPort tokenHasherPort;
    private final String publicBaseUrl;

    public ShareService(
            ShareRepositoryPort shareRepositoryPort,
            TreatmentRecordRepositoryPort treatmentRecordRepositoryPort,
            SecureTokenGeneratorPort tokenGeneratorPort,
            TokenHasherPort tokenHasherPort,
            @Value("${app.share.public-base-url}") String publicBaseUrl
    ) {
        this.shareRepositoryPort = shareRepositoryPort;
        this.treatmentRecordRepositoryPort = treatmentRecordRepositoryPort;
        this.tokenGeneratorPort = tokenGeneratorPort;
        this.tokenHasherPort = tokenHasherPort;
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    @Override
    @Transactional
    public CreateShareUseCase.Result create(CreateShareUseCase.Command command) {
        // 도메인 팩터리가 선택 불변식(대상 1 이상 + 항목 1 이상)과 유효기간을 먼저 통과시킨다.
        String rawToken = tokenGeneratorPort.generate();
        Share share = Share.create(command.userId(), tokenHash(rawToken), command.recordIds(),
                command.savedStyleIds(), command.fields(), command.expiresInDays(), Instant.now());
        requireOwnedRecords(command.userId(), command.recordIds());
        Share saved = shareRepositoryPort.insert(share);
        return new CreateShareUseCase.Result(saved, publicBaseUrl + "/" + rawToken);
    }

    @Override
    public ListSharesUseCase.Result list(ListSharesUseCase.Query query) {
        if (query.page() < 0 || query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
        SharePage page = shareRepositoryPort.findPage(
                query.requesterId(), query.status(), query.page(), query.size());
        return new ListSharesUseCase.Result(
                page.items(), query.page(), query.size(), page.totalElements());
    }

    @Override
    public Share get(GetShareUseCase.Query query) {
        return ownedShare(query.requesterId(), query.shareId());
    }

    @Override
    @Transactional
    public Share update(UpdateShareUseCase.Command command) {
        Share current = ownedShare(command.requesterId(), command.shareId());
        // 전달한 필드만 바꾼다. 미전달 필드는 현재 값을 그대로 둔다.
        Share updated = current.update(
                command.fields().orElse(current.fields()),
                command.expiresAt().orElse(current.expiresAt()),
                Instant.now());
        return shareRepositoryPort.update(updated);
    }

    @Override
    @Transactional
    public void delete(DeleteShareUseCase.Command command) {
        Share current = ownedShare(command.requesterId(), command.shareId());
        // 행을 지우지 않는다. 철회 이력 자체가 감사 증적이고, 공개 조회 차단도 상태 전이로 충분하다.
        shareRepositoryPort.update(current.revoke(Instant.now()));
    }

    /** 소유자 조건까지 걸어 조회한다. 남의 공유는 없는 공유와 같은 404 다(#31 컨벤션). */
    private Share ownedShare(UUID requesterId, UUID shareId) {
        return shareRepositoryPort.findByIdAndUserId(shareId, requesterId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void requireOwnedRecords(UUID userId, Set<UUID> recordIds) {
        for (UUID recordId : recordIds) {
            treatmentRecordRepositoryPort.findByIdAndUserId(recordId, userId)
                    .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        }
    }

    private String tokenHash(String rawToken) {
        return tokenHasherPort.hash(rawToken);
    }
}
