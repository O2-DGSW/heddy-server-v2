package com.heddy.adapter.in.web.style;

import com.heddy.global.filter.RequestIdFilter;
import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StyleApiIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString(
            "70000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString(
            "70000000-0000-4000-8000-000000000002");
    private static final UUID BANG_TAG_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID SECOND_BANG_TAG_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000002");
    private static final UUID LONG_TAG_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000001");
    private static final UUID SECOND_LONG_TAG_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000002");
    private static final UUID UPDO_TAG_ID = UUID.fromString(
            "60000000-0000-4000-8000-000000000001");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpUsers() {
        insertUser(USER_ID, "style-user@example.com");
        insertUser(OTHER_USER_ID, "other-style-user@example.com");
    }

    @Test
    void getsSeededStyleTagsFromPostgresByCategory() throws Exception {
        mockMvc.perform(get("/style-tags")
                        .with(authentication(userAuthentication(USER_ID)))
                        .param("category", "BANG")
                        .header(RequestIdFilter.HEADER, "request-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(4))
                .andExpect(jsonPath("$.data.items[*].tag_name", containsInAnyOrder(
                        "시스루뱅", "풀뱅", "사이드뱅", "처피뱅")))
                .andExpect(jsonPath("$.data.items[*].category", containsInAnyOrder(
                        "BANG", "BANG", "BANG", "BANG")))
                .andExpect(jsonPath("$.request_id").value("request-21"));
    }

    @Test
    void getsAllSeededStyleTagsWhenCategoryIsNotSpecified() throws Exception {
        mockMvc.perform(get("/style-tags")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(24));
    }

    @Test
    void documentsStylePreferenceLimitsAndBearerAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type")
                        .value("http"))
                .andExpect(jsonPath("$['paths']['/me/style-preferences']['put']"
                        + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$.components.schemas.StylePreferencesRequest.properties"
                        + ".preferred_tag_ids.description")
                        .value(containsString("최대 10개")))
                .andExpect(jsonPath("$.components.schemas.StylePreferencesRequest.properties"
                        + ".excluded_tag_ids.description")
                        .value(containsString("최대 10개")));
    }

    @Test
    void savesGetsAndFullyReplacesCurrentUsersPreferences() throws Exception {
        savePreferences(USER_ID, BANG_TAG_ID, LONG_TAG_ID);

        mockMvc.perform(get("/me/style-preferences")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferred_tag_ids[0]")
                        .value(BANG_TAG_ID.toString()))
                .andExpect(jsonPath("$.data.excluded_tag_ids[0]")
                        .value(LONG_TAG_ID.toString()));

        mockMvc.perform(put("/me/style-preferences")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "preferred_tag_ids":["60000000-0000-4000-8000-000000000001"],
                                  "excluded_tag_ids":[]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferred_tag_ids[0]")
                        .value(UPDO_TAG_ID.toString()))
                .andExpect(jsonPath("$.data.excluded_tag_ids").isEmpty());

        Integer savedCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_style_preferences WHERE user_id = ?",
                Integer.class, USER_ID);
        assertThat(savedCount).isEqualTo(1);
    }

    @Test
    void scopesPreferencesToAuthenticatedUser() throws Exception {
        savePreferences(USER_ID, BANG_TAG_ID, LONG_TAG_ID);
        savePreferences(OTHER_USER_ID, UPDO_TAG_ID, BANG_TAG_ID);

        mockMvc.perform(get("/me/style-preferences")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferred_tag_ids[0]")
                        .value(BANG_TAG_ID.toString()))
                .andExpect(jsonPath("$.data.excluded_tag_ids[0]")
                        .value(LONG_TAG_ID.toString()));
    }

    @Test
    void returnsPutAndGetPreferencesInTheSameDeterministicOrder() throws Exception {
        String requestBody = preferenceBody(
                List.of(SECOND_BANG_TAG_ID, BANG_TAG_ID),
                List.of(SECOND_LONG_TAG_ID, LONG_TAG_ID));

        mockMvc.perform(put("/me/style-preferences")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferred_tag_ids[0]")
                        .value(BANG_TAG_ID.toString()))
                .andExpect(jsonPath("$.data.preferred_tag_ids[1]")
                        .value(SECOND_BANG_TAG_ID.toString()))
                .andExpect(jsonPath("$.data.excluded_tag_ids[0]")
                        .value(LONG_TAG_ID.toString()))
                .andExpect(jsonPath("$.data.excluded_tag_ids[1]")
                        .value(SECOND_LONG_TAG_ID.toString()));

        mockMvc.perform(get("/me/style-preferences")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferred_tag_ids[0]")
                        .value(BANG_TAG_ID.toString()))
                .andExpect(jsonPath("$.data.preferred_tag_ids[1]")
                        .value(SECOND_BANG_TAG_ID.toString()))
                .andExpect(jsonPath("$.data.excluded_tag_ids[0]")
                        .value(LONG_TAG_ID.toString()))
                .andExpect(jsonPath("$.data.excluded_tag_ids[1]")
                        .value(SECOND_LONG_TAG_ID.toString()));
    }

    @Test
    void returnsDocumentedErrorWhenMoreThanTenUniqueTagsAreRequested() throws Exception {
        List<UUID> elevenTagIds = IntStream.range(0, 11)
                .mapToObj(index -> UUID.randomUUID())
                .toList();

        mockMvc.perform(put("/me/style-preferences")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(preferenceBody(elevenTagIds, List.of())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code")
                        .value("STYLE_PREFERENCE_LIMIT_EXCEEDED"));
    }

    @Test
    void countsUniqueTagIdsWhenEnforcingTheLimit() throws Exception {
        List<UUID> tenTagIds = jdbcTemplate.queryForList(
                "SELECT style_tag_id FROM style_tags ORDER BY style_tag_id LIMIT 10",
                UUID.class);
        List<UUID> elevenEntriesWithDuplicate = new ArrayList<>(tenTagIds);
        elevenEntriesWithDuplicate.add(tenTagIds.getFirst());

        mockMvc.perform(put("/me/style-preferences")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(preferenceBody(
                                elevenEntriesWithDuplicate, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferred_tag_ids.length()").value(10));
    }

    @Test
    void rejectsConflictingTagsWithoutChangingExistingSettings() throws Exception {
        savePreferences(USER_ID, BANG_TAG_ID, LONG_TAG_ID);

        mockMvc.perform(put("/me/style-preferences")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(preferenceBody(
                                List.of(BANG_TAG_ID), List.of(BANG_TAG_ID))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("STYLE_PREFERENCE_CONFLICT"))
                .andExpect(jsonPath("$.error.field_errors[0].reason")
                        .value("DUPLICATED_WITH_EXCLUDED_TAGS"));

        assertSavedPreferenceCount(2);
    }

    @Test
    void rejectsUnknownTagsAsFieldValidationWithoutChangingExistingSettings()
            throws Exception {
        UUID unknownTagId = UUID.fromString(
                "99999999-9999-4999-8999-999999999999");
        savePreferences(USER_ID, BANG_TAG_ID, LONG_TAG_ID);

        mockMvc.perform(put("/me/style-preferences")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(preferenceBody(List.of(unknownTagId), List.of())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.field_errors[0].field")
                        .value("preferred_tag_ids"))
                .andExpect(jsonPath("$.error.field_errors[0].reason")
                        .value("STYLE_TAG_NOT_FOUND:" + unknownTagId));

        assertSavedPreferenceCount(2);
    }

    @Test
    void requiresAuthenticationForCurrentUsersPreferences() throws Exception {
        mockMvc.perform(get("/me/style-preferences"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDeletedAccountWhenGettingPreferences() throws Exception {
        updateAccountStatus(USER_ID, "DELETED");

        mockMvc.perform(get("/me/style-preferences")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AUTH_ACCOUNT_DELETED"));
    }

    @Test
    void rejectsDeletionPendingAccountWhenSavingPreferences() throws Exception {
        updateAccountStatus(USER_ID, "DELETION_PENDING");

        mockMvc.perform(put("/me/style-preferences")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(preferenceBody(List.of(), List.of())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AUTH_ACCOUNT_DELETED"));
    }

    @Test
    void createsListsUpdatesAndDeletesSavedStyle() throws Exception {
        String createdBody = mockMvc.perform(post("/saved-styles")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(RequestIdFilter.HEADER, "request-saved-style")
                        .content("""
                                {
                                  "style_name":"레이어드 커트",
                                  "image_url":"https://example.com/layered.jpg",
                                  "reason":"과거 만족도가 높은 커트와 유사함",
                                  "memo":"상담 때 보여주기"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.saved_style_id").isNotEmpty())
                .andExpect(jsonPath("$.data.style_name").value("레이어드 커트"))
                .andExpect(jsonPath("$.data.memo").value("상담 때 보여주기"))
                .andExpect(jsonPath("$.request_id").value("request-saved-style"))
                .andReturn().getResponse().getContentAsString();
        String savedStyleId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createdBody).at("/data/saved_style_id").asText();

        mockMvc.perform(get("/saved-styles")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].saved_style_id").value(savedStyleId))
                .andExpect(jsonPath("$.data.page.total_elements").value(1));

        mockMvc.perform(patch("/saved-styles/{savedStyleId}", savedStyleId)
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":\"다음 방문에 보여주기\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memo").value("다음 방문에 보여주기"));

        mockMvc.perform(patch("/saved-styles/{savedStyleId}", savedStyleId)
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memo").doesNotExist());

        mockMvc.perform(delete("/saved-styles/{savedStyleId}", savedStyleId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM saved_styles WHERE user_id = ?",
                Integer.class, USER_ID)).isZero();
    }

    @Test
    void deletingSavedStyleAlsoRemovesItFromExistingShare() throws Exception {
        UUID savedStyleId = createSavedStyle(
                USER_ID, "공유 후보", "https://example.com/shared.jpg");
        UUID shareId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO shares (share_id, user_id, token_hash, status, expires_at)
                VALUES (?, ?, ?, 'ACTIVE', now() + interval '1 day')
                """, shareId, USER_ID,
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", ""));
        jdbcTemplate.update("""
                INSERT INTO share_saved_styles (share_id, saved_style_id) VALUES (?, ?)
                """, shareId, savedStyleId);

        mockMvc.perform(delete("/saved-styles/{savedStyleId}", savedStyleId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM share_saved_styles WHERE saved_style_id = ?",
                Integer.class, savedStyleId)).isZero();
    }

    @Test
    void rejectsDuplicateSavedStyleAndKeepsOtherUsersCandidatesPrivate() throws Exception {
        createSavedStyle(USER_ID, "보브", "https://example.com/bob.jpg");

        mockMvc.perform(post("/saved-styles")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(savedStyleBody("보브", "https://example.com/bob.jpg")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SAVED_STYLE_DUPLICATED"));

        UUID otherStyleId = createSavedStyle(
                OTHER_USER_ID, "숏컷", "https://example.com/short.jpg");
        mockMvc.perform(patch("/saved-styles/{savedStyleId}", otherStyleId)
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":\"가져오기\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(delete("/saved-styles/{savedStyleId}", otherStyleId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsTwentyFirstSavedStyle() throws Exception {
        for (int index = 0; index < 20; index++) {
            createSavedStyle(USER_ID, "스타일 " + index,
                    "https://example.com/style-" + index + ".jpg");
        }

        mockMvc.perform(post("/saved-styles")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(savedStyleBody("초과", "https://example.com/overflow.jpg")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("SAVED_STYLE_LIMIT_EXCEEDED"));
    }

    @Test
    void requiresMemoFieldForSavedStylePatch() throws Exception {
        UUID savedStyleId = createSavedStyle(
                USER_ID, "레이어드", "https://example.com/layered-2.jpg");

        mockMvc.perform(patch("/saved-styles/{savedStyleId}", savedStyleId)
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    private void savePreferences(UUID userId, UUID preferredTagId, UUID excludedTagId)
            throws Exception {
        mockMvc.perform(put("/me/style-preferences")
                        .with(authentication(userAuthentication(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(preferenceBody(
                                List.of(preferredTagId), List.of(excludedTagId))))
                .andExpect(status().isOk());
    }

    private UUID createSavedStyle(UUID userId, String styleName, String imageUrl)
            throws Exception {
        String response = mockMvc.perform(post("/saved-styles")
                        .with(authentication(userAuthentication(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(savedStyleBody(styleName, imageUrl)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).at("/data/saved_style_id").asText());
    }

    private String savedStyleBody(String styleName, String imageUrl) {
        return """
                {
                  "style_name":"%s",
                  "image_url":"%s",
                  "reason":"추천 이유"
                }
                """.formatted(styleName, imageUrl);
    }

    private UsernamePasswordAuthenticationToken userAuthentication(UUID userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private String preferenceBody(List<UUID> preferredTagIds, List<UUID> excludedTagIds) {
        return """
                {
                  "preferred_tag_ids":[%s],
                  "excluded_tag_ids":[%s]
                }
                """.formatted(uuidJson(preferredTagIds), uuidJson(excludedTagIds));
    }

    private String uuidJson(List<UUID> tagIds) {
        return tagIds.stream()
                .map(tagId -> "\"" + tagId + "\"")
                .collect(Collectors.joining(","));
    }

    private void assertSavedPreferenceCount(int expectedCount) {
        Integer savedCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_style_preferences WHERE user_id = ?",
                Integer.class, USER_ID);
        assertThat(savedCount).isEqualTo(expectedCount);
    }

    private void insertUser(UUID userId, String email) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, ?, 'EMAIL', 'ACTIVE', 0)
                """, userId, email, "hash");
    }

    private void updateAccountStatus(UUID userId, String status) {
        jdbcTemplate.update(
                "UPDATE users SET status = ? WHERE user_id = ?", status, userId);
    }
}
