package com.heddy.domain.analysis.exception;

public class AnalysisException extends RuntimeException {

    private final AnalysisError error;

    public AnalysisException(AnalysisError error) {
        super(error.message());
        this.error = error;
    }

    public AnalysisError error() {
        return error;
    }
}
