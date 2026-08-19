package com.heddy.global.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidV7Test {

    @Test
    @DisplayName("버전 7 UUID 를 생성한다")
    void generatesVersion7() {
        assertThat(UuidV7.generate().version()).isEqualTo(7);
    }

    @Test
    @DisplayName("생성 순서대로 정렬된다")
    void isTimeOrdered() {
        UUID previous = UuidV7.generate();
        for (int i = 0; i < 1_000; i++) {
            UUID current = UuidV7.generate();
            assertThat(unsignedCompare(previous, current)).isNegative();
            previous = current;
        }
    }

    @Test
    @DisplayName("중복을 만들지 않는다")
    void isUnique() {
        Set<UUID> generated = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            assertThat(generated.add(UuidV7.generate())).isTrue();
        }
    }

    private static int unsignedCompare(UUID left, UUID right) {
        int high = Long.compareUnsigned(left.getMostSignificantBits(), right.getMostSignificantBits());
        return high != 0 ? high : Long.compareUnsigned(left.getLeastSignificantBits(), right.getLeastSignificantBits());
    }
}
