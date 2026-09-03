package com.heddy.application.style.service;

import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import com.heddy.domain.sharing.port.out.ShareRepositoryPort;
import com.heddy.domain.style.exception.StyleError;
import com.heddy.domain.style.exception.StyleException;
import com.heddy.domain.style.model.CatalogHairstyle;
import com.heddy.domain.style.model.HairColor;
import com.heddy.domain.style.model.SavedStyle;
import com.heddy.domain.style.port.in.SavedStyleUseCase;
import com.heddy.domain.style.port.out.HairColorRepositoryPort;
import com.heddy.domain.style.port.out.HairstyleCatalogLookupPort;
import com.heddy.domain.style.port.out.SavedStyleRepositoryPort;
import com.heddy.global.error.ApplicationException;
import com.heddy.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 저장한 후보 스타일 보관함. 카탈로그(스타일·색상)를 가리키는 참조와 저장 시점 캡처를 묶어
 * 화면 한 장을 조립한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedStyleService implements SavedStyleUseCase {

    private static final int MAX_SAVED_STYLES = 20;

    private final SavedStyleRepositoryPort savedStyleRepositoryPort;
    private final HairColorRepositoryPort hairColorRepositoryPort;
    private final HairstyleCatalogLookupPort hairstyleCatalogLookupPort;
    private final FileRepositoryPort fileRepositoryPort;
    private final FileStoragePort fileStoragePort;
    private final ShareRepositoryPort shareRepositoryPort;

    /**
     * 보관함은 {@link #MAX_SAVED_STYLES} 개가 상한이라 한 번에 다 내려도 부담이 없다.
     * 페이지를 나누면 화면이 얻는 것 없이 계약만 복잡해져 목록을 통째로 돌려준다.
     */
    @Override
    public List<Item> list(UUID requesterId) {
        List<SavedStyle> savedStyles = savedStyleRepositoryPort.findAllByUserId(requesterId);
        Map<UUID, HairColor> colors = colorsOf(savedStyles);
        Map<UUID, UUID> thumbnails = thumbnailFileIdsOf(savedStyles);
        return savedStyles.stream()
                .map(style -> new Item(
                        style,
                        style.colorId() == null ? null : colors.get(style.colorId()),
                        imageUrl(style, thumbnails)))
                .toList();
    }

    @Override
    @Transactional
    public Item save(SaveCommand command) {
        List<SavedStyle> existing = savedStyleRepositoryPort
                .findAllByUserId(command.requesterId());
        boolean duplicated = existing.stream().anyMatch(saved ->
                Objects.equals(saved.hairstyleId(), command.hairstyleId())
                        && Objects.equals(saved.colorId(), command.colorId()));
        if (duplicated) {
            throw new StyleException(StyleError.SAVED_STYLE_DUPLICATED);
        }
        if (existing.size() >= MAX_SAVED_STYLES) {
            throw new StyleException(StyleError.SAVED_STYLE_LIMIT_EXCEEDED);
        }
        CatalogHairstyle hairstyle = requireActiveHairstyle(command.hairstyleId());
        HairColor color = command.colorId() == null ? null : requireColor(command.colorId());
        if (command.captureId() != null) {
            requireOwnedReadyCapture(command.requesterId(), command.captureId());
        }
        SavedStyle saved = savedStyleRepositoryPort.insert(SavedStyle.fromCatalog(
                command.requesterId(), hairstyle.hairstyleId(), hairstyle.styleName(),
                command.colorId(), command.captureId(), command.memo()));
        // Map.of 는 null 값을 거부한다. 썸네일이 없는 카탈로그 항목도 저장할 수 있어야 한다.
        Map<UUID, UUID> thumbnail = hairstyle.thumbnailFileId() == null
                ? Map.of() : Map.of(hairstyle.hairstyleId(), hairstyle.thumbnailFileId());
        return new Item(saved, color, imageUrl(saved, thumbnail));
    }

    @Override
    @Transactional
    public Item updateMemo(UpdateMemoCommand command) {
        if (!command.memoPresent()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
        SavedStyle current = savedStyleRepositoryPort
                .findByIdAndUserId(command.savedStyleId(), command.requesterId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        SavedStyle updated = savedStyleRepositoryPort.update(current.updateMemo(command.memo()));
        HairColor color = updated.colorId() == null
                ? null : hairColorRepositoryPort.findById(updated.colorId()).orElse(null);
        return new Item(updated, color, imageUrl(updated, thumbnailFileIdsOf(List.of(updated))));
    }

    /**
     * 후보를 지우면 그 후보를 실은 공유에서도 함께 빠진다. 링크를 깨는 대신 사라진 내용만
     * 조용히 빼는 것이 기록 삭제와 같은 처리다. 캡처 파일은 정리 대상으로 표시한다.
     */
    @Override
    @Transactional
    public void delete(UUID requesterId, UUID savedStyleId) {
        SavedStyle savedStyle = savedStyleRepositoryPort
                .findByIdAndUserId(savedStyleId, requesterId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        shareRepositoryPort.detachSavedStyle(savedStyleId);
        if (savedStyle.captureId() != null) {
            fileRepositoryPort.findById(savedStyle.captureId()).ifPresent(file -> {
                if (file.status() != FileStatus.DELETED) {
                    fileRepositoryPort.transition(file.markDeleted(), file.status());
                }
            });
        }
        if (!savedStyleRepositoryPort.deleteById(savedStyleId)) {
            throw new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private CatalogHairstyle requireActiveHairstyle(UUID hairstyleId) {
        return hairstyleCatalogLookupPort.findActiveById(hairstyleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private HairColor requireColor(UUID colorId) {
        return hairColorRepositoryPort.findById(colorId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    /** 캡처는 요청자 소유의 업로드를 마친 AR 캡처여야 한다. 시술기록 사진 첨부와 같은 규칙이다. */
    private void requireOwnedReadyCapture(UUID requesterId, UUID captureId) {
        StoredFile file = fileRepositoryPort.findById(captureId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!requesterId.equals(file.userId())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN_RESOURCE);
        }
        if (!file.isReady() || file.purpose() != FilePurpose.AR_CAPTURE) {
            throw new ApplicationException(ErrorCode.FILE_INVALID_STATE);
        }
    }

    private Map<UUID, HairColor> colorsOf(List<SavedStyle> savedStyles) {
        Map<UUID, HairColor> colors = new HashMap<>();
        for (SavedStyle style : savedStyles) {
            if (style.colorId() != null) {
                // 내려간 색도 그대로 보여 준다. 이름을 못 그리는 것보다 낫다.
                colors.computeIfAbsent(style.colorId(),
                        id -> hairColorRepositoryPort.findById(id).orElse(null));
            }
        }
        colors.values().removeIf(java.util.Objects::isNull);
        return colors;
    }

    private Map<UUID, UUID> thumbnailFileIdsOf(List<SavedStyle> savedStyles) {
        Set<UUID> hairstyleIds = savedStyles.stream()
                .map(SavedStyle::hairstyleId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return hairstyleCatalogLookupPort.findThumbnailFileIds(hairstyleIds);
    }

    /**
     * 카드에 띄울 이미지. AR 로 찍은 캡처가 있으면 그것이 우선이고, 없으면 AI 추천 스냅샷에
     * 저장된 URL, 그것도 없으면 카탈로그 썸네일 순으로 내려간다. URL 은 보관하지 않고 조회
     * 시점에 짧은 만료로 발급한다.
     */
    private URI imageUrl(SavedStyle savedStyle, Map<UUID, UUID> thumbnailFileIds) {
        URI capture = signedUrl(savedStyle.captureId());
        if (capture != null) {
            return capture;
        }
        if (savedStyle.imageUrl() != null) {
            return URI.create(savedStyle.imageUrl());
        }
        return signedUrl(thumbnailFileIds.get(savedStyle.hairstyleId()));
    }

    private URI signedUrl(UUID fileId) {
        if (fileId == null) {
            return null;
        }
        return fileRepositoryPort.findById(fileId)
                .filter(StoredFile::isReady)
                .map(fileStoragePort::createDownloadUrl)
                .orElse(null);
    }
}
