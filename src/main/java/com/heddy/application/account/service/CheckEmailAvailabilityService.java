package com.heddy.application.account.service;

import com.heddy.domain.account.port.in.CheckEmailAvailabilityUseCase;
import com.heddy.domain.account.port.in.EmailAvailabilityResult;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckEmailAvailabilityService implements CheckEmailAvailabilityUseCase {

    private final AccountRepositoryPort accountRepositoryPort;

    @Override
    public EmailAvailabilityResult check(String email) {
        return new EmailAvailabilityResult(email, !accountRepositoryPort.existsByEmail(email));
    }
}
