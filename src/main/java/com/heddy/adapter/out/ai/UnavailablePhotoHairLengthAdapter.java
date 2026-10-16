package com.heddy.adapter.out.ai;

import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.recommendation.model.PhotoHairEvidence;
import com.heddy.domain.recommendation.port.out.PhotoHairLengthPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.photo-length.enabled", havingValue = "false", matchIfMissing = true)
public class UnavailablePhotoHairLengthAdapter implements PhotoHairLengthPort {
    public PhotoHairEvidence analyze(StoredFile file) {
        return new PhotoHairEvidence(file.fileId(), null, "DISABLED", null, false);
    }
}
