package com.heddy.domain.style.port.in;

import com.heddy.domain.style.model.HairColor;
import com.heddy.domain.style.model.SavedStyle;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** 저장한 후보 스타일 보관함. AR로 시연한 스타일을 담아 두고 다시 꺼내 쓰는 자리다. */
public interface SavedStyleUseCase {

    Page list(UUID requesterId, int page, int size);

    Item save(SaveCommand command);

    Item updateMemo(UpdateMemoCommand command);

    void delete(UUID requesterId, UUID savedStyleId);

    record Item(SavedStyle savedStyle, HairColor color, URI imageUrl) {
    }

    record Page(List<Item> items, long totalElements) {
    }

    record SaveCommand(
            UUID requesterId,
            UUID hairstyleId,
            UUID colorId,
            UUID captureId,
            String memo
    ) {
    }

    record UpdateMemoCommand(
            UUID requesterId,
            UUID savedStyleId,
            boolean memoPresent,
            String memo
    ) {
    }
}
