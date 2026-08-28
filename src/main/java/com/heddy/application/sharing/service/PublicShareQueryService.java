package com.heddy.application.sharing.service;

import com.heddy.domain.account.port.out.TokenHasherPort;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import com.heddy.domain.sharing.exception.SharingError;
import com.heddy.domain.sharing.exception.SharingException;
import com.heddy.domain.sharing.model.Share;
import com.heddy.domain.sharing.model.ShareFieldType;
import com.heddy.domain.sharing.model.ShareStatus;
import com.heddy.domain.sharing.model.SharedContentSnapshot;
import com.heddy.domain.sharing.model.SharedContentSnapshot.RecordSnapshot;
import com.heddy.domain.sharing.model.SharedContentView;
import com.heddy.domain.sharing.model.SharedContentView.SharedPhotoView;
import com.heddy.domain.sharing.model.SharedContentView.SharedRecordView;
import com.heddy.domain.sharing.model.SharedContentView.SharedSavedStyleView;
import com.heddy.domain.sharing.port.in.GetPublicShareUseCase;
import com.heddy.domain.sharing.port.out.ShareRepositoryPort;
import com.heddy.domain.sharing.port.out.SharedContentPort;
import com.heddy.domain.style.model.SavedStyle;
import com.heddy.domain.style.port.out.SavedStyleRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 무인증 공개 조회. 토큰 해시 대조 → 철회·만료 검증(스펙 19절 매 요청) → 선택 항목 게이트를
 * 통과한 내용만 조립한다. 미선택 필드는 이 단계에서 이미 null 로 두어 직렬화부터 제외되고,
 * 사진은 READY 인 파일에만 조회 시점 URL 을 발급한다.
 */
@Service
@Transactional(readOnly = true)
public class PublicShareQueryService implements GetPublicShareUseCase {

    private final ShareRepositoryPort shareRepositoryPort;
    private final SharedContentPort sharedContentPort;
    private final SavedStyleRepositoryPort savedStyleRepositoryPort;
    private final FileRepositoryPort fileRepositoryPort;
    private final FileStoragePort fileStoragePort;
    private final TokenHasherPort tokenHasherPort;

    public PublicShareQueryService(
            ShareRepositoryPort shareRepositoryPort,
            SharedContentPort sharedContentPort,
            SavedStyleRepositoryPort savedStyleRepositoryPort,
            FileRepositoryPort fileRepositoryPort,
            FileStoragePort fileStoragePort,
            TokenHasherPort tokenHasherPort
    ) {
        this.shareRepositoryPort = shareRepositoryPort;
        this.sharedContentPort = sharedContentPort;
        this.savedStyleRepositoryPort = savedStyleRepositoryPort;
        this.fileRepositoryPort = fileRepositoryPort;
        this.fileStoragePort = fileStoragePort;
        this.tokenHasherPort = tokenHasherPort;
    }

    @Override
    public Result get(Query query) {
        Share share = shareRepositoryPort.findByTokenHash(tokenHasherPort.hash(query.shareToken()))
                // 없는 링크와 틀린 링크를 구분하지 않는다. 존재 여부 자체가 정보다.
                .orElseThrow(() -> new SharingException(SharingError.TOKEN_INVALID));
        Instant now = Instant.now();
        if (share.status() == ShareStatus.REVOKED) {
            throw new SharingException(SharingError.REVOKED);
        }
        if (share.isExpired(now)) {
            throw new SharingException(SharingError.EXPIRED);
        }

        Set<ShareFieldType> fields = share.fields();
        SharedContentSnapshot loaded = sharedContentPort.load(share.userId(), share.recordIds());
        List<SharedRecordView> gated = new ArrayList<>(loaded.records().size());
        for (RecordSnapshot record : loaded.records()) {
            gated.add(gate(record, fields));
        }
        return new Result(share.expiresAt(),
                fields.contains(ShareFieldType.SAVED_STYLES),
                new SharedContentView(loaded.ownerDisplayName(), List.copyOf(gated),
                        savedStyles(share, fields)));
    }

    private List<SharedSavedStyleView> savedStyles(
            Share share,
            Set<ShareFieldType> fields
    ) {
        if (!fields.contains(ShareFieldType.SAVED_STYLES)) {
            return null;
        }
        return savedStyleRepositoryPort
                .findAllByUserIdAndIds(share.userId(), share.savedStyleIds())
                .stream()
                .map(PublicShareQueryService::toView)
                .toList();
    }

    private static SharedSavedStyleView toView(SavedStyle savedStyle) {
        return new SharedSavedStyleView(
                savedStyle.styleName(), savedStyle.imageUrl(), savedStyle.reason());
    }

    /** 선택 항목이 아닌 값은 버린다. 남아 있는 것은 화면에 나갈 값뿐이다. */
    private SharedRecordView gate(RecordSnapshot record, Set<ShareFieldType> fields) {
        boolean details = fields.contains(ShareFieldType.TREATMENT_DETAILS);
        return new SharedRecordView(
                details ? record.performedAt() : null,
                details ? record.salonName() : null,
                details ? record.designerName() : null,
                details ? record.serviceTypes() : null,
                orNull(fields.contains(ShareFieldType.SATISFACTION), record.satisfaction()),
                orNull(fields.contains(ShareFieldType.MEMO), record.memo()),
                orNull(fields.contains(ShareFieldType.CAUTIONS), record.nextVisitCautions()),
                // 미선택이면 빈 배열이 아니라 null — 키 자체를 빼는 게 스펙이다.
                fields.contains(ShareFieldType.PHOTOS)
                        ? photos(record) : null);
    }

    /** READY 가 아닌 파일은 아예 목록에서 뺀다. 깨진 링크를 내보내지 않기 위해서다. */
    private List<SharedPhotoView> photos(RecordSnapshot record) {
        return record.photos().stream()
                .filter(SharedContentSnapshot.PhotoSnapshot::ready)
                .flatMap(photo -> downloadUrl(photo)
                        .map(uri -> new SharedPhotoView(photo.imageType(), uri))
                        .stream())
                .toList();
    }

    private Optional<URI> downloadUrl(SharedContentSnapshot.PhotoSnapshot photo) {
        return fileRepositoryPort.findById(photo.fileId())
                .filter(file -> file.status() == FileStatus.READY)
                .map(fileStoragePort::createDownloadUrl);
    }

    private <T> T orNull(boolean include, T value) {
        return include ? value : null;
    }
}
