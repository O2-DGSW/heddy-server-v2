package com.heddy.domain.recommendation.port.out;

import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.recommendation.model.PhotoHairEvidence;

public interface PhotoHairLengthPort {
    PhotoHairEvidence analyze(StoredFile file);
}
