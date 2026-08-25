package com.heddy.domain.sharing.model;

import com.heddy.domain.sharing.exception.SharingError;
import com.heddy.domain.sharing.exception.SharingException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShareTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TOKEN_HASH = "a".repeat(64);

    @Test
    void createsShareWithDefaultSevenDayExpiry() {
        Share share = Share.create(USER_ID, TOKEN_HASH,
                Set.of(UUID.randomUUID()), Set.of(), fields(), null, NOW);

        assertThat(share.status()).isEqualTo(ShareStatus.ACTIVE);
        assertThat(share.expiresAt()).isEqualTo(NOW.plusSeconds(7 * 86_400));
        assertThat(share.revokedAt()).isNull();
    }

    @Test
    void acceptsSavedStylesAloneWithoutAnyRecord() {
        Share share = Share.create(USER_ID, TOKEN_HASH,
                Set.of(), Set.of(UUID.randomUUID()), Set.of(ShareFieldType.SAVED_STYLES), null, NOW);

        assertThat(share.savedStyleIds()).hasSize(1);
        assertThat(share.recordIds()).isEmpty();
    }

    @Test
    void rejectsSelectionWithoutRecordsAndSavedStyles() {
        assertThatThrownBy(() -> Share.create(USER_ID, TOKEN_HASH,
                Set.of(), Set.of(), fields(), null, NOW))
                .isInstanceOfSatisfying(SharingException.class,
                        e -> assertThat(e.error()).isEqualTo(SharingError.EMPTY_SELECTION));
    }

    @Test
    void rejectsSelectionWithoutFields() {
        assertThatThrownBy(() -> Share.create(USER_ID, TOKEN_HASH,
                Set.of(UUID.randomUUID()), Set.of(), Set.of(), null, NOW))
                .isInstanceOfSatisfying(SharingException.class,
                        e -> assertThat(e.error()).isEqualTo(SharingError.EMPTY_SELECTION));
    }

    @Test
    void rejectsBlankTokenHash() {
        assertThatThrownBy(() -> Share.create(USER_ID, " ",
                Set.of(UUID.randomUUID()), Set.of(), fields(), null, NOW))
                .isInstanceOfSatisfying(SharingException.class,
                        e -> assertThat(e.error()).isEqualTo(SharingError.TOKEN_HASH_REQUIRED));
    }

    @Test
    void rejectsNonPositiveExpiryDays() {
        assertThatThrownBy(() -> Share.create(USER_ID, TOKEN_HASH,
                Set.of(UUID.randomUUID()), Set.of(), fields(), 0, NOW))
                .isInstanceOfSatisfying(SharingException.class,
                        e -> assertThat(e.error()).isEqualTo(SharingError.EXPIRES_IN_DAYS_INVALID));
    }

    @Test
    void treatsExpiryMomentItselfAsExpired() {
        Share share = Share.create(USER_ID, TOKEN_HASH,
                Set.of(UUID.randomUUID()), Set.of(), fields(), 1, NOW);

        assertThat(share.isExpired(NOW.plusSeconds(86_400))).isTrue();
        assertThat(share.isExpired(NOW.plusSeconds(86_399))).isFalse();
    }

    @Test
    void isNotViewableWhenRevokedOrExpired() {
        Share active = Share.create(USER_ID, TOKEN_HASH,
                Set.of(UUID.randomUUID()), Set.of(), fields(), 1, NOW);
        Share revoked = active.revoke(NOW.plusSeconds(60));

        assertThat(active.isViewable(NOW)).isTrue();
        assertThat(revoked.status()).isEqualTo(ShareStatus.REVOKED);
        assertThat(revoked.revokedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(revoked.isViewable(NOW.plusSeconds(60))).isFalse();
        assertThat(active.isViewable(NOW.plusSeconds(2 * 86_400))).isFalse();
    }

    @Test
    void revokeIsIdempotentAndKeepsFirstRevocationMoment() {
        Share share = Share.create(USER_ID, TOKEN_HASH,
                Set.of(UUID.randomUUID()), Set.of(), fields(), 1, NOW);
        Instant firstRevokeAt = NOW.plusSeconds(10);

        Share revokedTwice = share.revoke(firstRevokeAt).revoke(NOW.plusSeconds(9999));

        assertThat(revokedTwice.revokedAt()).isEqualTo(firstRevokeAt);
    }

    @Test
    void updatesFieldsAndExpiryButNeverTargetsOrToken() {
        Share original = Share.create(USER_ID, TOKEN_HASH,
                Set.of(UUID.randomUUID()), Set.of(), fields(), 1, NOW);
        Set<ShareFieldType> newFields = Set.of(ShareFieldType.MEMO);

        Share updated = original.update(newFields, NOW.plusSeconds(3 * 86_400),
                NOW.plusSeconds(60));

        assertThat(updated.fields()).isEqualTo(newFields);
        assertThat(updated.expiresAt()).isEqualTo(NOW.plusSeconds(3 * 86_400));
        assertThat(updated.recordIds()).isEqualTo(original.recordIds());
        assertThat(updated.savedStyleIds()).isEqualTo(original.savedStyleIds());
        assertThat(updated.tokenHash()).isEqualTo(original.tokenHash());
    }

    @Test
    void refusesUpdateToPastOrPresentExpiry() {
        Share share = Share.create(USER_ID, TOKEN_HASH,
                Set.of(UUID.randomUUID()), Set.of(), fields(), 1, NOW);
        Instant requestTime = NOW.plusSeconds(60);

        assertThatThrownBy(() -> share.update(fields(), NOW, requestTime))
                .isInstanceOfSatisfying(SharingException.class,
                        e -> assertThat(e.error()).isEqualTo(SharingError.EXPIRES_AT_NOT_FUTURE));
    }

    private static Set<ShareFieldType> fields() {
        return Set.of(ShareFieldType.PHOTOS, ShareFieldType.TREATMENT_DETAILS);
    }
}
