package com.heddy.adapter.in.web.account;

import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthOpenApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void loginSchemasDoNotExposeDeviceInformation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.EmailLoginRequest.properties.email").exists())
                .andExpect(jsonPath("$.components.schemas.EmailLoginRequest.properties.password").exists())
                .andExpect(jsonPath("$.components.schemas.EmailLoginRequest.properties.device").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.EmailLoginRequest.required",
                        not(hasItem("device"))))
                .andExpect(jsonPath("$.components.schemas.SocialLoginRequest.properties.provider").exists())
                .andExpect(jsonPath("$.components.schemas.SocialLoginRequest.properties.provider_token").exists())
                .andExpect(jsonPath("$.components.schemas.SocialLoginRequest.properties.device").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.SocialLoginRequest.required",
                        not(hasItem("device"))));
    }
}
