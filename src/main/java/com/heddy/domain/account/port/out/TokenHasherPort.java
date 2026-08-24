package com.heddy.domain.account.port.out;

public interface TokenHasherPort {
    String hash(String rawToken);
}
