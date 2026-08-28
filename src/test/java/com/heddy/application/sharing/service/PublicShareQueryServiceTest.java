package com.heddy.application.sharing.service;

import com.heddy.domain.account.port.out.TokenHasherPort;
import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import com.heddy.domain.sharing.exception.SharingError;
import com.heddy.domain.sharing.exception.SharingException;
import com.heddy.domain.sharing.model.Share;
import com.heddy.domain.sharing.model.ShareFieldType;
import com.heddy.domain.sharing.model.ShareStatus;
import com.heddy.domain.sharing.model.SharedContentSnapshot;
import com.heddy.domain.sharing.model.SharedContentSnapshot.PhotoSnapshot;
import com.heddy.domain.sharing.model.SharedContentSnapshot.RecordSnapshot;
import com.heddy.domain.sharing.model.SharedContentView.SharedRecordView;
import com.heddy.domain.sharing.port.in.GetPublicShareUseCase;
import com.heddy.domain.sharing.port.out.ShareRepositoryPort;
import com.heddy.domain.sharing.port.out.SharedContentPort;
import com.heddy.domain.style.model.SavedStyle;
import com.heddy.domain.style.port.out.SavedStyleRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PublicShareQueryServiceTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID FILE_ID = UUID.randomUUID();
    private static final UUID SAVED_STYLE_ID = UUID.randomUUID();
    private static final String RAW_TOKEN = "raw-public-token";
    private static final String TOKEN_HASH = "hash-" + RAW_TOKEN;
    private static final Instant NOW = Instant.now();

    @Mock ShareRepositoryPort shareRepositoryPort;
    @Mock SharedContentPort sharedContentPort;
    @Mock SavedStyleRepositoryPort savedStyleRepositoryPort;
    @Mock FileRepositoryPort fileRepositoryPort;
    @Mock FileStoragePort fileStoragePort;
    @Mock TokenHasherPort tokenHasherPort;

    private PublicShareQueryService service;

    @BeforeEach
    void setUp() {
        service = new PublicShareQueryService(shareRepositoryPort, sharedContentPort,
                savedStyleRepositoryPort, fileRepositoryPort, fileStoragePort, tokenHasherPort);
    }

    @Test
    void comparesTheHashNeverTheRawToken() {
        stubActiveShare(Set.of(ShareFieldType.TREATMENT_DETAILS));
        given(sharedContentPort.load(OWNER_ID, Set.of(RECORD_ID)))
                .willReturn(new SharedContentSnapshot("준헤어", List.of()));

        GetPublicShareUseCase.Result result = service.get(query());

        then(tokenHasherPort).should().hash(RAW_TOKEN);
        then(shareRepositoryPort).should().findByTokenHash(TOKEN_HASH);
        assertThat(result.content().ownerDisplayName()).isEqualTo("준헤어");
        assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(86_400));
    }

    @Test
    void deniesUnknownTokensWithoutRevealingExistence() {
        given(tokenHasherPort.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(shareRepositoryPort.findByTokenHash(TOKEN_HASH)).willReturn(Optional.empty());

        assertThatThrownBy(this::getOrFail)
                .isInstanceOfSatisfying(SharingException.class, e -> {
                    assertThat(e.error()).isEqualTo(SharingError.TOKEN_INVALID);
                    assertThat(e.error().code()).isEqualTo("SHARE_TOKEN_INVALID");
                });
        then(sharedContentPort).shouldHaveNoInteractions();
    }

    @Test
    void verifiesRevocationBeforeExpiryOnEveryRequest() {
        Share revoked = Share.reconstitute(UUID.randomUUID(), OWNER_ID, TOKEN_HASH,
                ShareStatus.REVOKED, NOW.minusSeconds(60), NOW.minusSeconds(120),
                Set.of(RECORD_ID), fields(), Set.of(), NOW.minusSeconds(86_400));
        given(tokenHasherPort.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(shareRepositoryPort.findByTokenHash(TOKEN_HASH))
                .willReturn(Optional.of(revoked));

        assertThatThrownBy(this::getOrFail)
                .isInstanceOfSatisfying(SharingException.class, e ->
                        assertThat(e.error()).isEqualTo(SharingError.REVOKED));

        // 만료됐어도 철회가 먼저다. 사용자에게는 철회 사실이 더 정확한 안내다.
        Share revokedAndExpired = Share.reconstitute(UUID.randomUUID(), OWNER_ID, TOKEN_HASH,
                ShareStatus.REVOKED, NOW.minusSeconds(3600), NOW.minusSeconds(120),
                Set.of(RECORD_ID), fields(), Set.of(), NOW.minusSeconds(2 * 86_400));
        given(shareRepositoryPort.findByTokenHash(TOKEN_HASH))
                .willReturn(Optional.of(revokedAndExpired));

        assertThatThrownBy(this::getOrFail)
                .isInstanceOfSatisfying(SharingException.class, e ->
                        assertThat(e.error()).isEqualTo(SharingError.REVOKED));
    }

    @Test
    void refusesExpiredLinks() {
        Share expired = Share.reconstitute(UUID.randomUUID(), OWNER_ID, TOKEN_HASH,
                ShareStatus.ACTIVE, NOW.minusSeconds(1), null,
                Set.of(RECORD_ID), fields(), Set.of(), NOW.minusSeconds(86_400));
        given(tokenHasherPort.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(shareRepositoryPort.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(expired));

        assertThatThrownBy(this::getOrFail)
                .isInstanceOfSatisfying(SharingException.class, e ->
                        assertThat(e.error()).isEqualTo(SharingError.EXPIRED));
    }

    @Test
    void gatesUnselectedFieldsOutOfTheResponse() {
        stubActiveShare(Set.of(ShareFieldType.PHOTOS));
        given(sharedContentPort.load(OWNER_ID, Set.of(RECORD_ID)))
                .willReturn(new SharedContentSnapshot("gangmin", List.of(
                        new RecordSnapshot(NOW, "준헤어", "김실장", Set.of("CUT"), 4,
                                "메모", "주의사항",
                                List.of(new PhotoSnapshot("AFTER", FILE_ID, true))))));
        StoredFile ready = readyFile(FILE_ID);
        given(fileRepositoryPort.findById(FILE_ID)).willReturn(Optional.of(ready));
        given(fileStoragePort.createDownloadUrl(ready))
                .willReturn(URI.create("https://signed/get"));

        GetPublicShareUseCase.Result result = getOrFail();

        SharedRecordView record = result.content().records().getFirst();
        assertThat(record.photos()).hasSize(1);
        // 선택하지 않은 항목은 값이 아니라 키부터 빠지도록 null 로 내보낸다.        assertThat(record.performedAt()).isNull();
        assertThat(record.salonName()).isNull();
        assertThat(record.designerName()).isNull();
        assertThat(record.satisfaction()).isNull();
        assertThat(record.memo()).isNull();
        assertThat(record.nextVisitCautions()).isNull();
        assertThat(result.content().savedStyles()).isNull();
    }

    @Test
    void issuesShortLivedUrlsOnlyForReadyPhotos() {
        UUID pendingFileId = UUID.randomUUID();
        stubActiveShare(
                Set.of(ShareFieldType.PHOTOS, ShareFieldType.SAVED_STYLES),
                Set.of(SAVED_STYLE_ID));
        given(sharedContentPort.load(OWNER_ID, Set.of(RECORD_ID)))
                .willReturn(new SharedContentSnapshot("gangmin", List.of(
                        new RecordSnapshot(NOW, null, null, null, null, null, null,
                                List.of(
                                        new PhotoSnapshot("AFTER", FILE_ID, true),
                                        new PhotoSnapshot("BEFORE", pendingFileId, false))))));
        StoredFile ready = readyFile(FILE_ID);
        given(fileRepositoryPort.findById(FILE_ID)).willReturn(Optional.of(ready));
        given(fileStoragePort.createDownloadUrl(ready)).willReturn(URI.create("https://signed/get"));
        given(savedStyleRepositoryPort.findAllByUserIdAndIds(
                OWNER_ID, Set.of(SAVED_STYLE_ID)))
                .willReturn(List.of(new SavedStyle(
                        SAVED_STYLE_ID, OWNER_ID, "레이어드 커트",
                        "https://images.example.com/layered.jpg", "추천 이유", NOW)));

        GetPublicShareUseCase.Result result = getOrFail();

        SharedRecordView record = result.content().records().getFirst();
        assertThat(record.photos()).hasSize(1);
        assertThat(record.photos().getFirst().displayUrl())
                .isEqualTo(URI.create("https://signed/get"));
        assertThat(result.content().savedStyles()).singleElement().satisfies(style -> {
            assertThat(style.styleName()).isEqualTo("레이어드 커트");
            assertThat(style.imageUrl()).isEqualTo("https://images.example.com/layered.jpg");
            assertThat(style.reason()).isEqualTo("추천 이유");
        });
    }

    @Test
    void dropsDeletedRecordsQuietlyInsteadOfBreakingTheLink() {
        stubActiveShare(fields());
        given(sharedContentPort.load(OWNER_ID, Set.of(RECORD_ID)))
                .willReturn(new SharedContentSnapshot("gangmin", List.of()));

        GetPublicShareUseCase.Result result = getOrFail();

        assertThat(result.content().records()).isEmpty();
        assertThat(result.content().ownerDisplayName()).isEqualTo("gangmin");
    }

    // ------------------------------------------------------------------ 헬퍼

    private GetPublicShareUseCase.Query query() {
        return new GetPublicShareUseCase.Query(RAW_TOKEN);
    }

    private GetPublicShareUseCase.Result getOrFail() {
        return service.get(query());
    }

    private Set<ShareFieldType> fields() {
        return Set.of(ShareFieldType.PHOTOS, ShareFieldType.TREATMENT_DETAILS);
    }

    private void stubActiveShare(Set<ShareFieldType> fields) {
        stubActiveShare(fields, Set.of());
    }

    private void stubActiveShare(
            Set<ShareFieldType> fields,
            Set<UUID> savedStyleIds
    ) {
        given(tokenHasherPort.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(shareRepositoryPort.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(
                Share.reconstitute(UUID.randomUUID(), OWNER_ID, TOKEN_HASH,
                        ShareStatus.ACTIVE, NOW.plusSeconds(86_400), null,
                        Set.of(RECORD_ID), fields, savedStyleIds, NOW)));
    }

    private StoredFile readyFile(UUID fileId) {
        return new StoredFile(fileId, UUID.randomUUID(), OWNER_ID, FilePurpose.TREATMENT_PHOTO,
                FileStatus.READY, "object-key", "image/jpeg", "after.jpg", 1024L,
                null, null, null, Instant.now().plusSeconds(60), Instant.now(), null);
    }
}
