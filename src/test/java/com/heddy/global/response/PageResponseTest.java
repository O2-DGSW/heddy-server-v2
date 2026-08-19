package com.heddy.global.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    private final JsonMapper objectMapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    @Test
    void exposesSpecPageMeta() {
        PageResponse<String> response = PageResponse.of(
                new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 20), 43));

        assertThat(response.items()).containsExactly("a", "b");
        assertThat(response.page()).isEqualTo(new PageResponse.PageMeta(0, 20, 43, 3, true));
    }

    @Test
    void mapsElementsWhileKeepingPageMeta() {
        PageResponse<Integer> response = PageResponse.of(
                new PageImpl<>(List.of("a", "bb"), PageRequest.of(1, 2), 4), String::length);

        assertThat(response.items()).containsExactly(1, 2);
        assertThat(response.page().hasNext()).isFalse();
    }

    @Test
    void serializesPageMetaInSnakeCase() {
        String json = objectMapper.writeValueAsString(
                PageResponse.of(new PageImpl<>(List.of("a"), PageRequest.of(0, 20), 43)));

        assertThat(json).contains("\"total_elements\":43", "\"total_pages\":3", "\"has_next\":true");
    }
}
