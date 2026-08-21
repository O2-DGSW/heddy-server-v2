package com.heddy.domain.style.exception;

public class StyleException extends RuntimeException {

    private final StyleError error;

    public StyleException(StyleError error) {
        super(error.message());
        this.error = error;
    }

    public StyleError error() {
        return error;
    }
}
