package com.heddy.domain.file.exception;

public class FileException extends RuntimeException {

    private final FileError error;

    public FileException(FileError error) {
        super(error.message());
        this.error = error;
    }

    public FileError error() {
        return error;
    }
}
