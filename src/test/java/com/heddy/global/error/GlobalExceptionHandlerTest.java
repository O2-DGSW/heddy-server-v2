package com.heddy.global.error;

import com.heddy.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(GlobalExceptionHandlerTest.ProbeController.class)
@Import({GlobalExceptionHandlerTest.ProbeController.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void applicationExceptionUsesErrorCodeStatusAndName() throws Exception {
        mockMvc.perform(get("/probe/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("요청한 리소스를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.error.field_errors").doesNotExist())
                .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    /** 도메인 enum 이 ErrorCode 를 구현하면 공통 핸들러가 그대로 처리한다. */
    @Test
    void applicationExceptionCarriesDomainCodeAndFieldErrors() throws Exception {
        mockMvc.perform(get("/probe/conflict"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("PROBE_CONFLICT"))
                .andExpect(jsonPath("$.error.field_errors[0].field").value("preferred_tag_ids"))
                .andExpect(jsonPath("$.error.field_errors[0].reason").value("DUPLICATED_WITH_EXCLUDED_TAGS"));
    }

    @Test
    void beanValidationFailureIs422ValidationFailed() throws Exception {
        mockMvc.perform(post("/probe/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.field_errors[0].field").value("name"));
    }

    @Test
    void unreadableBodyIs400InvalidRequest() throws Exception {
        mockMvc.perform(post("/probe/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void missingRequiredParameterIs400InvalidRequest() throws Exception {
        mockMvc.perform(get("/probe/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void authenticationFailureIs401() throws Exception {
        mockMvc.perform(get("/probe/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
    }

    /** 인증 인프라 장애(DB·Redis)는 자격 증명 실패가 아니라 서버 오류다. 401로 삼키면 안 된다. */
    @Test
    void authenticationInfrastructureFailureIs500() throws Exception {
        mockMvc.perform(get("/probe/auth-infra-down"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    void keepsMessageKeyWhenMessageIsBlank() throws Exception {
        mockMvc.perform(get("/probe/blank-message"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").exists())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void keepsMessageKeyWhenMessageIsNull() throws Exception {
        mockMvc.perform(get("/probe/null-message"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.hasKey("message")));
    }

    @Test
    void unexpectedExceptionIs500() throws Exception {
        mockMvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));
    }

    @RestController
    @RequestMapping("/probe")
    static class ProbeController {

        @GetMapping("/not-found")
        void notFound() {
            throw new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ApplicationException(
                    ProbeErrorCode.PROBE_CONFLICT,
                    ProbeErrorCode.PROBE_CONFLICT.message(),
                    List.of(new ApiErrorResponse.FieldError("preferred_tag_ids", "DUPLICATED_WITH_EXCLUDED_TAGS")));
        }

        @PostMapping("/echo")
        ApiResponse<EchoRequest> echo(@Valid @RequestBody EchoRequest request) {
            return ApiResponse.of(request);
        }

        @GetMapping("/search")
        ApiResponse<String> search(@RequestParam String keyword) {
            return ApiResponse.of(keyword);
        }

        @GetMapping("/bad-credentials")
        void badCredentials() {
            throw new BadCredentialsException("자격 증명이 올바르지 않습니다.");
        }

        @GetMapping("/auth-infra-down")
        void authInfraDown() {
            throw new InternalAuthenticationServiceException(
                    "user lookup failed", new IllegalStateException("db down"));
        }

        @GetMapping("/blank-message")
        void blankMessage() {
            throw new ApplicationException(CommonErrorCode.INVALID_REQUEST, "");
        }

        @GetMapping("/null-message")
        ResponseEntity<ApiErrorResponse> nullMessage() {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.of(CommonErrorCode.INVALID_REQUEST, null));
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("boom");
        }

        record EchoRequest(@NotBlank String name) {
        }

        enum ProbeErrorCode implements ErrorCode {

            PROBE_CONFLICT(HttpStatus.UNPROCESSABLE_CONTENT, "프로브 규칙 위반입니다.");

            private final HttpStatus status;
            private final String message;

            ProbeErrorCode(HttpStatus status, String message) {
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
}
