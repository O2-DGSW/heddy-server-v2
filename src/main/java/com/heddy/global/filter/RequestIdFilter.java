package com.heddy.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청마다 추적 ID를 확정해 MDC와 응답 헤더에 싣는다.
 *
 * <p>클라이언트가 {@code X-Request-Id}를 보내면 그 값을 쓰고, 없으면 UUID를 새로 발급한다.
 * 응답 본문의 {@code request_id}는 {@link #currentRequestId()}를 통해 이 값을 읽는다.
 *
 * <p>보안 필터 체인보다 먼저 실행돼야 인증 실패 응답에도 추적 ID가 실리므로 최우선 순위로 등록한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    /** 외부에서 주입된 값을 그대로 로그·응답에 싣지 않도록 길이와 문자 집합을 제한한다. */
    private static final int MAX_LENGTH = 64;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolve(request.getHeader(REQUEST_ID_HEADER));
        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /** 현재 요청의 추적 ID. 필터 밖(스케줄러·테스트 등)에서 호출하면 {@code null}이다. */
    public static String currentRequestId() {
        return MDC.get(MDC_KEY);
    }

    private static String resolve(String header) {
        if (!StringUtils.hasText(header) || header.length() > MAX_LENGTH || !isSafe(header)) {
            return UUID.randomUUID().toString();
        }
        return header;
    }

    private static boolean isSafe(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean allowed = Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }
}
