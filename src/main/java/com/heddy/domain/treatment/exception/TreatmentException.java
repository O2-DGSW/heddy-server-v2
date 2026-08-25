package com.heddy.domain.treatment.exception;

public class TreatmentException extends RuntimeException {

    private final TreatmentError error;

    public TreatmentException(TreatmentError error) {
        super(error.message());
        this.error = error;
    }

    public TreatmentError error() {
        return error;
    }
}
