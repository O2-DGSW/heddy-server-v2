package com.heddy.domain.file.service;

import com.heddy.domain.file.exception.FileError;
import com.heddy.domain.file.exception.FileException;
import com.heddy.domain.file.model.FilePurpose;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjectKeyGeneratorTest {

    private static final UUID OWNER_ID = UUID.randomUUID();

    @Test
    void buildsKeyFromPurposeOwnerAndContentType() {
        String key = ObjectKeyGenerator.generate(
                FilePurpose.TREATMENT_PHOTO, OWNER_ID, "image/jpeg");

        assertThat(key).startsWith("TREATMENT_PHOTO/" + OWNER_ID + "/").endsWith(".jpg");
    }

    @Test
    void givesEveryCallItsOwnKey() {
        String first = ObjectKeyGenerator.generate(FilePurpose.AR_CAPTURE, OWNER_ID, "image/png");
        String second = ObjectKeyGenerator.generate(FilePurpose.AR_CAPTURE, OWNER_ID, "image/png");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void keepsKeyInsideTheOwnerPrefixRegardlessOfCaller() {
        String key = ObjectKeyGenerator.generate(
                FilePurpose.TREATMENT_PHOTO, OWNER_ID, "image/heic");

        // 접두사를 벗어나는 경로 조각이 끼어들 자리가 없어야 한다. 파일명을 받지 않으니 구조가 고정된다.
        assertThat(key.split("/")).hasSize(3);
        assertThat(key).doesNotContain("..");
    }

    @Test
    void rejectsContentTypeWithoutKnownExtension() {
        assertThatThrownBy(() -> ObjectKeyGenerator.generate(
                FilePurpose.TREATMENT_PHOTO, OWNER_ID, "application/pdf"))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).error())
                .isEqualTo(FileError.CONTENT_TYPE_NOT_ALLOWED);
    }

    /**
     * 허용 목록과 확장자 매핑이 따로 있어서, 한쪽에만 형식을 추가하면 런타임에야 드러난다.
     * 그 어긋남을 빌드 시점에 잡는다.
     */
    @Test
    void mapsAnExtensionForEveryContentTypeAnyPurposeAllows() {
        for (FilePurpose purpose : FilePurpose.values()) {
            assertThat(ObjectKeyGenerator.mappedContentTypes())
                    .as("%s 가 허용하는 형식", purpose)
                    .containsAll(purpose.allowedContentTypes());
        }
    }
}
