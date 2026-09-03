package com.heddy.domain.style.port.in;

import com.heddy.domain.style.model.SavedStyle;
import com.heddy.domain.style.model.SavedStylePage;

import java.util.UUID;

public interface SavedStyleUseCase {

    SavedStyle create(CreateCommand command);

    SavedStylePage list(ListQuery query);

    SavedStyle updateMemo(UpdateMemoCommand command);

    void delete(DeleteCommand command);

    record CreateCommand(
            UUID userId,
            String styleName,
            String imageUrl,
            String reason,
            String memo
    ) {
    }

    record ListQuery(UUID userId, int page, int size) {
    }

    record UpdateMemoCommand(
            UUID userId,
            UUID savedStyleId,
            boolean memoPresent,
            String memo
    ) {
    }

    record DeleteCommand(UUID userId, UUID savedStyleId) {
    }
}
