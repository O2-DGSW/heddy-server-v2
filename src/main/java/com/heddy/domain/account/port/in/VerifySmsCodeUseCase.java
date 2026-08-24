package com.heddy.domain.account.port.in;

public interface VerifySmsCodeUseCase {
    void verify(VerifySmsCodeCommand command);
}
