package com.heddy.domain.recommendation.model;

import com.heddy.domain.account.model.HairProfile.HairLength;
import java.util.List;
import java.util.UUID;

/** Local segmentation evidence. Shape contains hair occupancy only, never face embeddings or image bytes. */
public record PhotoHairEvidence(UUID fileId, String modelVersion, String status,
        HairLength observedLength, boolean applied, List<Double> hairShape, int comparedStyleCount) {
    public PhotoHairEvidence {
        hairShape = hairShape == null ? List.of() : List.copyOf(hairShape);
    }
    public PhotoHairEvidence(UUID fileId, String modelVersion, String status, HairLength length, boolean applied) {
        this(fileId, modelVersion, status, length, applied, List.of(), 0);
    }
    public static PhotoHairEvidence absent(String status) {
        return new PhotoHairEvidence(null, null, status, null, false);
    }
    public PhotoHairEvidence withApplied(boolean value) {
        return new PhotoHairEvidence(fileId, modelVersion, status, observedLength, value, hairShape, comparedStyleCount);
    }
    public PhotoHairEvidence withComparedStyles(int count) {
        return new PhotoHairEvidence(fileId, modelVersion, status, observedLength, applied, hairShape, count);
    }
}
