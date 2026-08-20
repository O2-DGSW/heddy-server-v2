package com.heddy.domain.account.port.in;

public interface CheckEmailAvailabilityUseCase {
    EmailAvailabilityResult check(String email);
}
