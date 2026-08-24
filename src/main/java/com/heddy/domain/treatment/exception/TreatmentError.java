package com.heddy.domain.treatment.exception;

public enum TreatmentError {
    SERVICE_TYPE_REQUIRED("TREATMENT_SERVICE_TYPE_REQUIRED", "시술 종류를 1개 이상 선택해야 합니다."),
    SERVICE_TYPE_UNKNOWN("TREATMENT_SERVICE_TYPE_UNKNOWN", "알 수 없는 시술 종류가 포함되어 있습니다."),
    PERFORMED_AT_IN_FUTURE("TREATMENT_PERFORMED_AT_IN_FUTURE", "시술일은 미래일 수 없습니다."),
    SATISFACTION_OUT_OF_RANGE("TREATMENT_SATISFACTION_OUT_OF_RANGE", "만족도는 1에서 5 사이여야 합니다."),
    PRICE_INCOMPLETE("TREATMENT_PRICE_INCOMPLETE", "가격은 금액과 통화를 함께 입력해야 합니다."),
    PRICE_AMOUNT_NEGATIVE("TREATMENT_PRICE_AMOUNT_NEGATIVE", "가격은 음수일 수 없습니다."),
    PRICE_CURRENCY_INVALID("TREATMENT_PRICE_CURRENCY_INVALID", "통화 코드는 3자리 알파벳이어야 합니다."),
    SALON_NAME_TOO_LONG("TREATMENT_SALON_NAME_TOO_LONG", "미용실 이름은 최대 50자까지 입력할 수 있습니다."),
    DESIGNER_NAME_TOO_LONG("TREATMENT_DESIGNER_NAME_TOO_LONG", "디자이너 이름은 최대 30자까지 입력할 수 있습니다."),
    PHOTO_LIMIT_EXCEEDED("TREATMENT_PHOTO_LIMIT_EXCEEDED", "사진은 기록당 최대 10장까지 등록할 수 있습니다."),
    PHOTO_RECORD_MISMATCH("TREATMENT_PHOTO_RECORD_MISMATCH", "다른 기록에 속한 사진은 등록할 수 없습니다.");

    private final String code;
    private final String message;

    TreatmentError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
