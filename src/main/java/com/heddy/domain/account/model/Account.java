package com.heddy.domain.account.model;

public record Account(
        Long id,
        String loginId,
        String encodedPassword,
        String name,
        String phoneNumber,
        AccountRole role,
        AccountStatus status,
        boolean phoneVerified
) {
    public static Account local(String loginId, String encodedPassword, String name, String phoneNumber) {
        return new Account(null, loginId, encodedPassword, name, phoneNumber,
                AccountRole.USER, AccountStatus.ACTIVE, true);
    }

    public static Account social(String name, String phoneNumber) {
        return new Account(null, null, null, name, phoneNumber,
                AccountRole.USER, AccountStatus.ACTIVE, true);
    }
}
