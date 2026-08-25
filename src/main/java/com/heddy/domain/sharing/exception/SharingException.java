package com.heddy.domain.sharing.exception;

public class SharingException extends RuntimeException {

    private final SharingError error;

    public SharingException(SharingError error) {
        super(error.message());
        this.error = error;
    }

    public SharingError error() {
        return error;
    }
}
