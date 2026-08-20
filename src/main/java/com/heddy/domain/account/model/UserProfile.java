package com.heddy.domain.account.model;

import java.util.UUID;

public record UserProfile(
        UUID userId,
        String nickname,
        String phone,
        String preferredDesigner,
        String hairCautions
) {
    public static UserProfile signup(UUID userId, String nickname) {
        return new UserProfile(userId, nickname, null, null, null);
    }

    public static UserProfile signup(UUID userId, String nickname, String phone) {
        return new UserProfile(userId, nickname, phone, null, null);
    }
}
