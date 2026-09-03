package com.heddy.global.error;

import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 잘못 부른 요청은 서버 장애가 아니다. 인증 필터가 먼저 401 을 돌려주는 탓에 토큰 없이
 * 호출할 때는 드러나지 않으므로, 인증된 요청으로 계약을 고정한다.
 */
@AutoConfigureMockMvc
class NotFoundHandlingIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString(
            "88000000-0000-4000-8000-0000000000ff");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void answersUnmappedPathWithNotFound() throws Exception {
        mockMvc.perform(get("/me/does-not-exist")
                        .with(authentication(userAuthentication())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void answersUnsupportedMethodWithMethodNotAllowedAndAllowHeader() throws Exception {
        // /me/consents 는 GET 만 받는다. 경로는 있고 메서드만 없는 상황이어야 405 다.
        mockMvc.perform(post("/me/consents")
                        .with(authentication(userAuthentication())))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }

    private UsernamePasswordAuthenticationToken userAuthentication() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
    }
}
