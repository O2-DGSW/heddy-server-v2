package com.heddy.infrastructure.security.jwt;

enum JwtTokenType {
    ACCESS("access"),
    REAUTHENTICATION("reauthentication");

    private final String value;

    JwtTokenType(String value) {
        this.value = value;
    }

    String value() {
        return value;
    }
}
