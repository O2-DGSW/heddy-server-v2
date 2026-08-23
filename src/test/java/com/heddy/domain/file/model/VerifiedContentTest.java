package com.heddy.domain.file.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class VerifiedContentTest {

    private static final String SHA256 = "b".repeat(64);

    @Test
    void acceptsSixtyFourCharacterHash() {
        assertThatCode(() -> new VerifiedContent("image/jpeg", 1_024, SHA256, 800, 600))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsHashOfWrongLength() {
        assertThatThrownBy(() -> new VerifiedContent("image/jpeg", 1_024, "abc", 800, 600))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingHash() {
        assertThatThrownBy(() -> new VerifiedContent("image/jpeg", 1_024, null, 800, 600))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveDimensions() {
        assertThatThrownBy(() -> new VerifiedContent("image/jpeg", 1_024, SHA256, 0, 600))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VerifiedContent("image/jpeg", 1_024, SHA256, 800, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
