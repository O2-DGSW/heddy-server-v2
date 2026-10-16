package com.heddy.adapter.out.ai;

import com.heddy.domain.file.model.*;
import com.heddy.domain.file.port.out.FileStoragePort;
import com.heddy.domain.account.model.HairProfile.HairLength;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OnnxPhotoHairLengthAdapterTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "PHOTO_LENGTH_TEST_IMAGES", matches = ".+")
    void realPinnedModelDistinguishesAuditedCatalogPhotosAndCachesObservations() throws Exception {
        FileStoragePort storage = mock(FileStoragePort.class);
        try (var adapter = new CloseableAdapter(storage)) {
            // Locally supplied, audited SYSTEM catalogue pictures. Never commit/export user photos.
            // Side/rear views and a face obscured by glasses/hands have insufficient geometry: uncertain.
            var expected = new HairLength[]{null, null, HairLength.SHORT,
                    null, null, HairLength.SHORT};
            for (int i = 0; i < expected.length; i++) {
                var file = new StoredFile(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        FilePurpose.TREATMENT_PHOTO, FileStatus.READY, "test", "image/jpeg", "test.jpg",
                        1024, null, null, null, Instant.now(), Instant.now(), null);
                byte[] bytes = Files.readAllBytes(Path.of(System.getenv("PHOTO_LENGTH_TEST_IMAGES"), i + ".jpg"));
                when(storage.readObject(eq(file), anyLong())).thenReturn(bytes);
                var evidence = adapter.delegate.analyze(file);
                assertThat(evidence.observedLength()).as("catalog image %s (%s)", i, evidence.status()).isEqualTo(expected[i]);
                assertThat(adapter.delegate.analyze(file)).isEqualTo(evidence);
                verify(storage, times(1)).readObject(eq(file), anyLong());
            }
        }
    }
    private static class CloseableAdapter implements AutoCloseable {
        final OnnxPhotoHairLengthAdapter delegate;
        CloseableAdapter(FileStoragePort storage) {
            delegate = new OnnxPhotoHairLengthAdapter(storage, System.getenv("PHOTO_LENGTH_TEST_MODEL"));
        }
        public void close() throws Exception { delegate.close(); }
    }
}
