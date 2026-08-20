package com.heddy.adapter.in.web.account.controller;

import com.heddy.config.TestSecurityConfig;
import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.account.model.HairProfile.HairCondition;
import com.heddy.domain.account.model.HairProfile.HairLength;
import com.heddy.domain.account.model.HairProfile.HairThickness;
import com.heddy.domain.account.model.HairProfile.HairType;
import com.heddy.domain.account.port.in.MyProfileResult;
import com.heddy.domain.account.port.in.ProfileUseCase;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.global.error.GlobalExceptionHandler;
import com.heddy.global.filter.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(ProfileController.class)
@Import({TestSecurityConfig.class, AccountExceptionHandler.class,
        GlobalExceptionHandler.class, RequestIdFilter.class})
class ProfileControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-18T05:30:00Z");

    @Autowired MockMvc mockMvc;
    @MockitoBean ProfileUseCase profileUseCase;
    @MockitoBean AuthTokenPort authTokenPort;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getsProfileWithDocumentedEnvelope() throws Exception {
        given(profileUseCase.getProfile(USER_ID)).willReturn(profileResult());

        mockMvc.perform(get("/me")
                        .with(authentication(userAuthentication()))
                        .header(RequestIdFilter.HEADER, "request-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.preferred_designer").value("김디자이너"))
                .andExpect(jsonPath("$.data.hair_cautions").value("두피 자극 주의"))
                .andExpect(jsonPath("$.request_id").value("request-20"));
    }

    @Test
    void patchDistinguishesExplicitNullFromOmittedFields() throws Exception {
        given(profileUseCase.updateProfile(any())).willReturn(profileResult());

        mockMvc.perform(patch("/me")
                        .with(authentication(userAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":null}"))
                .andExpect(status().isOk());

        verify(profileUseCase).updateProfile(org.mockito.ArgumentMatchers.argThat(command ->
                command.userId().equals(USER_ID)
                        && command.phonePresent()
                        && command.phone() == null
                        && !command.nicknamePresent()));
    }

    @Test
    void rejectsNonCanonicalPhoneFormat() throws Exception {
        mockMvc.perform(patch("/me")
                        .with(authentication(userAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"010-1111-2222\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void savesHairProfileUsingSnakeCaseContract() throws Exception {
        given(profileUseCase.saveHairProfile(any())).willReturn(hairProfile());

        mockMvc.perform(put("/me/hair-profile")
                        .with(authentication(userAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hair_type":"WAVY",
                                  "hair_condition":"NORMAL",
                                  "hair_length":"BELOW_SHOULDER",
                                  "hair_thickness":"THICK",
                                  "available_care_time_minutes":15
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hair_type").value("WAVY"))
                .andExpect(jsonPath("$.data.available_care_time_minutes").value(15));
    }

    @Test
    void rejectsIncompleteHairProfile() throws Exception {
        mockMvc.perform(put("/me/hair-profile")
                        .with(authentication(userAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void returnsHairProfileNotFoundCode() throws Exception {
        given(profileUseCase.getHairProfile(USER_ID))
                .willThrow(new AccountException(AccountError.HAIR_PROFILE_NOT_FOUND));

        mockMvc.perform(get("/me/hair-profile")
                        .with(authentication(userAuthentication())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HAIR_PROFILE_NOT_FOUND"));
    }

    private UsernamePasswordAuthenticationToken userAuthentication() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, java.util.List.of());
    }

    private MyProfileResult profileResult() {
        return new MyProfileResult(USER_ID, "user@example.com", "헤디", "01012345678",
                "김디자이너", "두피 자극 주의", AccountStatus.ACTIVE, NOW, NOW);
    }

    private HairProfile hairProfile() {
        return new HairProfile(UUID.randomUUID(), USER_ID, HairType.WAVY,
                HairCondition.NORMAL, HairLength.BELOW_SHOULDER, HairThickness.THICK,
                15, NOW, NOW);
    }
}
