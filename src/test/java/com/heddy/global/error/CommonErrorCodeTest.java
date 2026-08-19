package com.heddy.global.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class CommonErrorCodeTest {

    @Test
    void codeIsAlwaysTheEnumName() {
        assertThat(CommonErrorCode.values())
                .allSatisfy(code -> assertThat(code.code()).isEqualTo(code.name()));
    }

    @Test
    void followsSpecStatusMapping() {
        assertThat(CommonErrorCode.INVALID_REQUEST.status().value()).isEqualTo(400);
        assertThat(CommonErrorCode.VALIDATION_FAILED.status().value()).isEqualTo(422);
        assertThat(CommonErrorCode.AUTHENTICATION_REQUIRED.status().value()).isEqualTo(401);
        assertThat(CommonErrorCode.ACCESS_TOKEN_EXPIRED.status().value()).isEqualTo(401);
        assertThat(CommonErrorCode.FORBIDDEN_RESOURCE.status().value()).isEqualTo(403);
        assertThat(CommonErrorCode.RESOURCE_NOT_FOUND.status().value()).isEqualTo(404);
        assertThat(CommonErrorCode.RATE_LIMIT_EXCEEDED.status().value()).isEqualTo(429);
        assertThat(CommonErrorCode.INTERNAL_SERVER_ERROR.status().value()).isEqualTo(500);
        assertThat(CommonErrorCode.DEPENDENCY_UNAVAILABLE.status().value()).isEqualTo(503);
    }

    @Test
    void definesOnlyTheNineCommonCodes() {
        assertThat(CommonErrorCode.values()).hasSize(9);
    }

    /** 도메인 PR 은 자기 패키지에서 이렇게 enum 을 추가한다. */
    @Test
    void domainEnumCanExtendErrorCode() {
        ErrorCode code = SampleDomainErrorCode.SAMPLE_NOT_FOUND;

        assertThat(code.code()).isEqualTo("SAMPLE_NOT_FOUND");
        assertThat(code.status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsNonEnumImplementation() {
        ErrorCode notAnEnum = new ErrorCode() {
            @Override
            public HttpStatus status() {
                return HttpStatus.BAD_REQUEST;
            }

            @Override
            public String message() {
                return "구현 금지";
            }
        };

        assertThatIllegalStateException().isThrownBy(notAnEnum::code);
    }

    enum SampleDomainErrorCode implements ErrorCode {

        SAMPLE_NOT_FOUND(HttpStatus.NOT_FOUND, "샘플을 찾을 수 없습니다.");

        private final HttpStatus status;
        private final String message;

        SampleDomainErrorCode(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        @Override
        public HttpStatus status() {
            return status;
        }

        @Override
        public String message() {
            return message;
        }
    }
}
