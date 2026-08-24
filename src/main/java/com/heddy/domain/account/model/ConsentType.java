package com.heddy.domain.account.model;

public enum ConsentType {
    TERMS_OF_SERVICE(true),
    PRIVACY_POLICY(true),
    AI_TRAINING(false),
    SERVICE_ANALYTICS(false),
    PUSH_NOTIFICATION(false),
    MARKETING_NOTIFICATION(false);

    private final boolean required;

    ConsentType(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }
}
