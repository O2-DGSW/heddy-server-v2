package com.heddy.domain.style.port.in;

import com.heddy.domain.style.model.HairColor;
import com.heddy.domain.style.model.SavedStyle;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** 저장한 후보 스타일 보관함. AR 로 시연한 스타일을 담아 두고 다시 꺼내 쓰는 자리다. */
public interface SavedStyleUseCase {

    List<Item> list(UUID requesterId);

    Item save(SaveCommand command);

    void delete(UUID requesterId, UUID savedStyleId);

    /**
     * 화면 한 장에 필요한 조합. 색상은 카탈로그에서 붙이고 이미지 URL 은 조회 시점에
     * 발급하므로, 저장한 후보 자체와 분리해 둔다.
     */
    record Item(SavedStyle savedStyle, HairColor color, URI imageUrl) {
    }

    record SaveCommand(
            UUID requesterId,
            UUID hairstyleId,
            UUID colorId,
            UUID captureId,
            String memo
    ) {
    }
}
