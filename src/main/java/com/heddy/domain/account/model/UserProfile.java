package com.heddy.domain.account.model;

import java.time.Instant;
import java.util.UUID;

public record UserProfile(
        UUID userId,
        String nickname,
        String phone,
        String preferredDesigner,
        String hairCautions,
        Instant createdAt,
        Instant updatedAt
) {
    public UserProfile(
            UUID userId,
            String nickname,
            String phone,
            String preferredDesigner,
            String hairCautions
    ) {
        this(userId, nickname, phone, preferredDesigner, hairCautions, null, null);
    }

    public static UserProfile signup(UUID userId, String nickname) {
        return new UserProfile(userId, nickname, null, null, null);
    }

    public static UserProfile signup(UUID userId, String nickname, String phone) {
        return new UserProfile(userId, nickname, phone, null, null);
    }

    public UserProfile update(
            String nickname,
            String phone,
            String preferredDesigner,
            String hairCautions
    ) {
        return new UserProfile(userId, nickname, phone, preferredDesigner, hairCautions,
                createdAt, updatedAt);
    }
}
