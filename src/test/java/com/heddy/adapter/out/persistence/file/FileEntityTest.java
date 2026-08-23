package com.heddy.adapter.out.persistence.file;

import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FileEntityTest {

    @Test
    void reportsAssignedIdEntityAsNewUntilPersistedOrLoaded() {
        FileEntity entity = new FileEntity(pendingFile());

        assertThat(entity.isNew()).isTrue();

        entity.markNotNew();

        assertThat(entity.isNew()).isFalse();
    }

    @Test
    void carriesMutableFieldsOnUpdateAndLeavesIdentityAlone() {
        StoredFile pending = pendingFile();
        FileEntity entity = new FileEntity(pending);

        entity.update(pending.markReady("image/png", 4_096));
        StoredFile updated = entity.toDomain();

        assertThat(entity.getId()).isEqualTo(pending.fileId());
        assertThat(updated.objectKey()).isEqualTo(pending.objectKey());
        assertThat(updated.status()).isEqualTo(FileStatus.READY);
        assertThat(updated.contentType()).isEqualTo("image/png");
        assertThat(updated.byteSize()).isEqualTo(4_096);
    }

    private static StoredFile pendingFile() {
        return StoredFile.pending(
                UUID.randomUUID(), FilePurpose.TREATMENT_PHOTO,
                "TREATMENT_PHOTO/owner/photo.jpg", "image/jpeg", 1_024);
    }
}
