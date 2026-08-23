package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.ConsentStatus;

import java.util.List;
import java.util.UUID;

public interface ConsentUseCase {
    List<ConsentStatus> getConsents(UUID userId);
    ConsentStatus changeConsent(ChangeConsentCommand command);
}
