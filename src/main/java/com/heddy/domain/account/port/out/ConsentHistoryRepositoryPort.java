package com.heddy.domain.account.port.out;

import com.heddy.domain.account.model.ConsentDecision;
import com.heddy.domain.account.model.ConsentSource;
import com.heddy.domain.account.model.ConsentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConsentHistoryRepositoryPort {
    List<ConsentStatus> findLatestByUserId(UUID userId);
    void append(UUID userId, List<ConsentDecision> decisions, ConsentSource source, Instant changedAt);
}
