package com.heddy.application.recommendation.service;

import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.account.model.HairProfile.HairLength;
import com.heddy.domain.file.model.*;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.recommendation.model.PhotoHairEvidence;
import com.heddy.domain.recommendation.port.out.PhotoHairLengthPort;
import com.heddy.domain.treatment.model.*;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RecommendationPhotoResolverTest {
    final UUID user = UUID.randomUUID();
    final Instant now = Instant.parse("2026-10-16T10:00:00Z");
    final TreatmentRecordRepositoryPort records = mock(TreatmentRecordRepositoryPort.class);
    final FileRepositoryPort files = mock(FileRepositoryPort.class);
    final PhotoHairLengthPort analyzer = mock(PhotoHairLengthPort.class);
    final RecommendationPhotoResolver resolver = new RecommendationPhotoResolver(records, files, analyzer);

    @Test void latestShortPhotoCapsOldLongProfileWithoutWritingIt() {
        var record = record(now, ImageType.AFTER, user);
        var original = profile(HairLength.BELOW_SHOULDER, now.minusSeconds(60));
        var resolved = resolver.resolve(user, original, List.of(record));
        assertThat(resolved.profile().hairLength()).isEqualTo(HairLength.SHORT);
        assertThat(resolved.evidence().applied()).isTrue();
        assertThat(original.hairLength()).isEqualTo(HairLength.BELOW_SHOULDER);
    }
    @Test void longerPhotoNeverOverridesExplicitShortProfile() {
        var record = record(now, ImageType.AFTER, user);
        when(analyzer.analyze(any())).thenAnswer(i -> new PhotoHairEvidence(((StoredFile)i.getArgument(0)).fileId(),
                "test", "ASSESSED", HairLength.BELOW_SHOULDER, false));
        var resolved = resolver.resolve(user, profile(HairLength.SHORT, now.minusSeconds(60)), List.of(record));
        assertThat(resolved.profile().hairLength()).isEqualTo(HairLength.SHORT);
        assertThat(resolved.evidence().applied()).isFalse();
    }
    @Test void newerExplicitProfileWinsAndSkipsInference() {
        var record = record(now, ImageType.AFTER, user);
        var resolved = resolver.resolve(user, profile(HairLength.BELOW_CHIN, now.plusSeconds(60)), List.of(record));
        assertThat(resolved.evidence().status()).isEqualTo("NEWER_PROFILE");
        verifyNoInteractions(analyzer);
    }
    @Test void neverUsesBeforePhotoOrAnotherAccountsFile() {
        var before = record(now, ImageType.BEFORE, user);
        assertThat(resolver.resolve(user, null, List.of(before)).evidence().applied()).isFalse();
        var foreign = record(now, ImageType.AFTER, UUID.randomUUID());
        assertThat(resolver.resolve(user, null, List.of(foreign)).evidence().applied()).isFalse();
        verifyNoInteractions(analyzer);
    }
    @Test void noPhotoOnLatestRecordDoesNotResurrectOldLength() {
        var old = record(now.minusSeconds(3600), ImageType.AFTER, user);
        var latest = record(now, ImageType.BEFORE, user);
        assertThat(resolver.resolve(user, null, List.of(old, latest)).evidence().status()).isEqualTo("NO_RECENT_PHOTO");
        verifyNoInteractions(analyzer);
    }
    @Test void uncertaintyPreservesProfileAndMissingProfileCanBeResolvedFromPhoto() {
        var record = record(now, ImageType.AFTER, user);
        assertThat(resolver.resolve(user, null, List.of(record)).profile().hairLength()).isEqualTo(HairLength.SHORT);
        when(analyzer.analyze(any())).thenReturn(PhotoHairEvidence.absent("UNCERTAIN"));
        var original = profile(HairLength.BELOW_CHIN, now.minusSeconds(60));
        assertThat(resolver.resolve(user, original, List.of(record)).profile()).isEqualTo(original);
    }
    private HairProfile profile(HairLength length, Instant updated) {
        return new HairProfile(UUID.randomUUID(), user, null, null, length, null, null, now.minusSeconds(3600), updated);
    }
    private TreatmentRecord record(Instant performed, ImageType type, UUID owner) {
        UUID id = UUID.randomUUID(), fileId = UUID.randomUUID();
        var photo = new TreatmentPhoto(UUID.randomUUID(), id, fileId, type, 0, performed);
        var record = new TreatmentRecord(id, user, Set.of(ServiceType.CUT), null, null, performed, 5,
                null, null, null, null, null, List.of(photo), performed);
        var file = new StoredFile(fileId, UUID.randomUUID(), owner, FilePurpose.TREATMENT_PHOTO,
                FileStatus.READY, "test", "image/jpeg", "test.jpg", 1024, null, null, null, now, performed, null);
        when(records.findByIdAndUserId(id, user)).thenReturn(Optional.of(record));
        when(files.findAllById(List.of(fileId))).thenReturn(List.of(file));
        lenient().when(analyzer.analyze(file)).thenReturn(new PhotoHairEvidence(fileId, "test", "ASSESSED", HairLength.SHORT, false));
        return record;
    }
}
