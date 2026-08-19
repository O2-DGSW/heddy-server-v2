package com.heddy.global.error;

import org.springframework.http.HttpStatus;

/**
 * API 명세 §18 오류 코드의 공통 계약.
 *
 * <p>공통 코드(§18.1)는 {@link CommonErrorCode}가 갖고, 도메인 코드는 각 도메인 패키지에서
 * {@code com.heddy.<도메인>.error.<도메인>ErrorCode} enum 으로 이 인터페이스를 구현해 추가한다.
 * 도메인 코드를 여기에 미리 몰아 정의하지 않는다 — 명세가 확정된 도메인만 자기 코드를 갖는다.
 *
 * <p>추가 규칙은 {@code docs/layer-convention.md} §4 "에러 응답" 참고.
 */
public interface ErrorCode {

    /**
     * 응답의 {@code error.code} 문자열. 구현 enum 의 이름을 그대로 쓴다.
     * 코드 문자열과 enum 이름이 갈리지 않도록 구현체는 반드시 enum 이어야 한다.
     */
    default String code() {
        if (this instanceof Enum<?> constant) {
            return constant.name();
        }
        throw new IllegalStateException("ErrorCode 는 enum 으로 구현한다: " + getClass().getName());
    }

    /** 이 코드가 내려갈 HTTP 상태. API 명세 §2.4 사용 기준을 따른다. */
    HttpStatus status();

    /** 클라이언트에 노출되는 기본 메시지. 개인정보·토큰·서명을 담지 않는다. */
    String message();
}
