package com.heddy.global.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 페이지 메타 계산만 검증한다. 실제 응답의 snake_case 직렬화는 전역 Jackson 설정에 달려 있어
 * {@code ApiContractIntegrationTest} 가 애플리케이션 컨텍스트로 확인한다.
 */
class PageResponseTest {

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

}
