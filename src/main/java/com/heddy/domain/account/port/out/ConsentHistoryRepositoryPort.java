package com.heddy.domain.account.port.out;

import com.heddy.domain.account.model.ConsentDecision;
import com.heddy.domain.account.model.ConsentSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConsentHistoryRepositoryPort {
    void append(UUID userId, List<ConsentDecision> decisions, ConsentSource source, Instant changedAt);
}
