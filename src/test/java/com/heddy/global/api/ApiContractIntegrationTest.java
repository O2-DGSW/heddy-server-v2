package com.heddy.global.api;

import com.heddy.global.response.ApiResponse;
import com.heddy.global.response.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 슬라이스 테스트가 닿지 못하는 경로 — 보안 필터 체인, 메서드 단위 인가, springdoc 문서, 전역 Jackson 설정 —
 * 을 실제 애플리케이션 컨텍스트로 검증한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ApiContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestReturnsSpecErrorBody() throws Exception {
        mockMvc.perform(get("/api/v1/probe/secured").header("X-Request-Id", "req-401"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", "req-401"))
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.error.message").isNotEmpty())
                .andExpect(jsonPath("$.request_id").value("req-401"));
    }

    @Test
    void methodSecurityDenialReturns403NotIntercepted() throws Exception {
        mockMvc.perform(get("/api/v1/probe/admin-only").header("X-Request-Id", "req-403").with(user("tester")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_RESOURCE"))
                .andExpect(jsonPath("$.request_id").value("req-403"));
    }

    @Test
    void serviceThrownAccessDeniedReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/probe/denied").with(user("tester")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_RESOURCE"));
    }

    @Test
    void wrongMethodOnExistingPathReturns405WithAllowHeader() throws Exception {
        mockMvc.perform(post("/api/v1/probe/page").with(user("tester")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("GET")))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    /** 전역 SNAKE_CASE 설정이 실제 응답에 걸리는지 — 테스트가 자기 ObjectMapper 를 만들지 않고 확인한다. */
    @Test
    void responseBodyUsesGlobalSnakeCaseNaming() throws Exception {
        mockMvc.perform(get("/api/v1/probe/page").with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.page.total_elements").value(43))
                .andExpect(jsonPath("$.data.page.total_pages").value(3))
                .andExpect(jsonPath("$.data.page.has_next").value(true))
                .andExpect(jsonPath("$.data.page.totalElements").doesNotExist())
                .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    /** springdoc 은 자기 ObjectMapper 를 쓰므로 스키마 프로퍼티명이 실제 응답과 갈리기 쉽다. */
    @Test
    void openApiSchemaUsesSnakeCaseNaming() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ApiResponseHealthResponse.properties.request_id").exists())
                .andExpect(jsonPath("$.components.schemas.ApiResponseHealthResponse.properties.requestId")
                        .doesNotExist());
    }

    @TestConfiguration
    static class ProbeConfiguration {

        @Bean
        ProbeController probeController() {
            return new ProbeController();
        }
    }

    @RestController
    @RequestMapping("/api/v1/probe")
    static class ProbeController {

        @GetMapping("/secured")
        ApiResponse<String> secured() {
            return ApiResponse.of("ok");
        }

        @GetMapping("/admin-only")
        @PreAuthorize("hasRole('NOPE')")
        ApiResponse<String> adminOnly() {
            return ApiResponse.of("ok");
        }

        @GetMapping("/denied")
        ApiResponse<String> denied() {
            throw new org.springframework.security.access.AccessDeniedException("소유자가 아닙니다.");
        }

        @GetMapping("/page")
        ApiResponse<PageResponse<String>> page() {
            return ApiResponse.of(PageResponse.of(
                    new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 20), 43)));
        }
    }
}
