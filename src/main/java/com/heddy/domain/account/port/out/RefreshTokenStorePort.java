package com.heddy.domain.account.port.out;

public interface RefreshTokenStorePort {
    void save(Long accountId, String token);
    void delete(Long accountId);
    boolean rotate(Long accountId, String expectedToken, String newToken);
}
