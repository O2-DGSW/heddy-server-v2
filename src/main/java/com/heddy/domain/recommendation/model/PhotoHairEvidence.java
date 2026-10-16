package com.heddy.domain.recommendation.model;

import com.heddy.domain.account.model.HairProfile.HairLength;
import java.util.UUID;

/** 길이 추정은 모발 건강 지표 분석과 별개이며, 성별은 추론하지 않는다. */
public record PhotoHairEvidence(UUID fileId, String modelVersion, String status,
                                HairLength observedLength, boolean applied) {
    public static PhotoHairEvidence absent(String status) {
        return new PhotoHairEvidence(null, null, status, null, false);
    }
    public PhotoHairEvidence withApplied(boolean value) {
        return new PhotoHairEvidence(fileId, modelVersion, status, observedLength, value);
    }
}
