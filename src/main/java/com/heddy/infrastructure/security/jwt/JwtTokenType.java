package com.heddy.infrastructure.security.jwt;

enum JwtTokenType {
    ACCESS("access"),
    REFRESH("refresh");

    private final String value;

    JwtTokenType(String value) {
        this.value = value;
    }

    String value() {
        return value;
    }
}
