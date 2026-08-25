package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.AccountDeletionRequest;

import java.util.UUID;

public interface RequestAccountDeletionUseCase {

    AccountDeletionRequest request(Command command);

    record Command(
            UUID userId,
            String reauthenticationToken,
            String reason
    ) {
    }
}
