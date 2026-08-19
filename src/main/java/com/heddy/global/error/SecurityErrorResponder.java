package com.heddy.global.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * 보안 필터 체인에서 발생한 401·403도 {@link GlobalExceptionHandler}와 같은 포맷으로 내려준다.
 * 이 단계는 {@code @RestControllerAdvice} 밖이라 직접 직렬화한다.
 */
@Component
public class SecurityErrorResponder implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) {
        write(response, CommonErrorCode.AUTHENTICATION_REQUIRED);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) {
        write(response, CommonErrorCode.FORBIDDEN_RESOURCE);
    }

    private void write(HttpServletResponse response, ErrorCode errorCode) {
        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        try {
            objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(errorCode));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
