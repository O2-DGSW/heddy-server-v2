package com.heddy.adapter.out.persistence.file;

import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FileEntityTest {

    @Test
    void carriesEveryDomainFieldThroughTheEntity() {
        StoredFile pending = StoredFile.pending(
                UUID.randomUUID(), FilePurpose.TREATMENT_PHOTO,
                "TREATMENT_PHOTO/user/photo.jpg", "image/jpeg", 1_024,
                Instant.parse("2026-08-23T12:00:00Z"));

        StoredFile roundTripped = new FileEntity(pending).toDomain();

        assertThat(roundTripped.fileId()).isEqualTo(pending.fileId());
        assertThat(roundTripped.uploadId()).isEqualTo(pending.uploadId());
        assertThat(roundTripped.userId()).isEqualTo(pending.userId());
        assertThat(roundTripped.purpose()).isEqualTo(FilePurpose.TREATMENT_PHOTO);
        assertThat(roundTripped.status()).isEqualTo(FileStatus.PENDING);
        assertThat(roundTripped.objectKey()).isEqualTo(pending.objectKey());
        assertThat(roundTripped.fileSize()).isEqualTo(1_024);
        assertThat(roundTripped.expiresAt()).isEqualTo(pending.expiresAt());
    }
}
