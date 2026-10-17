package com.heddy.application.recommendation.service;

import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.file.model.FileOwnerType;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.recommendation.model.PhotoHairEvidence;
import com.heddy.domain.recommendation.port.out.PhotoHairLengthPort;
import com.heddy.domain.treatment.model.ImageType;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class RecommendationPhotoResolver {
    private final TreatmentRecordRepositoryPort records;
    private final FileRepositoryPort files;
    private final PhotoHairLengthPort analyzer;
    public RecommendationPhotoResolver(TreatmentRecordRepositoryPort records,
            FileRepositoryPort files, PhotoHairLengthPort analyzer) {
        this.records = records; this.files = files; this.analyzer = analyzer;
    }

    public Resolved resolve(UUID userId, HairProfile profile, List<TreatmentRecord> recent) {
        if (recent.isEmpty()) return new Resolved(profile, PhotoHairEvidence.absent("NO_RECENT_PHOTO"));
        // Never use an older treatment merely because the newest record has no usable result photo.
        TreatmentRecord latest = recent.stream().max(Comparator.comparing(TreatmentRecord::performedAt)
                .thenComparing(TreatmentRecord::createdAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(TreatmentRecord::recordId)).orElseThrow();
        var record = records.findByIdAndUserId(latest.recordId(), userId).orElse(null);
        if (record == null) return new Resolved(profile, PhotoHairEvidence.absent("NO_RECENT_PHOTO"));
        // OTHER can hold an unclassified current photo. BEFORE must never describe the current result.
        var inputType = record.photos().stream().anyMatch(p -> p.imageType() == ImageType.AFTER)
                ? ImageType.AFTER : ImageType.OTHER;
        var afterIds = record.photos().stream().filter(p -> p.imageType() == inputType)
                .map(com.heddy.domain.treatment.model.TreatmentPhoto::fileId).distinct().toList();
        if (afterIds.isEmpty()) return new Resolved(profile, PhotoHairEvidence.absent("NO_RECENT_PHOTO"));
        // A newer upload replaces the former input, even if the old photo remains attached.
        var file = files.findAllById(afterIds).stream()
                .filter(f -> afterIds.contains(f.fileId()) && f.isReady() && f.ownerType() == FileOwnerType.USER
                        && f.purpose() == com.heddy.domain.file.model.FilePurpose.TREATMENT_PHOTO
                        && userId.equals(f.userId()))
                .max(Comparator.comparing(com.heddy.domain.file.model.StoredFile::createdAt,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(com.heddy.domain.file.model.StoredFile::fileId)).orElse(null);
        if (file == null) return new Resolved(profile, PhotoHairEvidence.absent("UNAVAILABLE"));
        PhotoHairEvidence evidence = analyzer.analyze(file);
        if (profile != null && profile.hairLength() != null && profile.updatedAt() != null
                && file.createdAt() != null && !profile.updatedAt().isBefore(file.createdAt())) {
            return new Resolved(profile, new PhotoHairEvidence(file.fileId(), evidence.modelVersion(),
                    "NEWER_PROFILE", evidence.observedLength(), false, evidence.hairShape(), 0));
        }
        var observed = evidence.observedLength();
        // An inferred longer length must never undo a user's explicit short-hair constraint.
        boolean apply = observed != null && (profile == null || profile.hairLength() == null
                || observed.ordinal() <= profile.hairLength().ordinal());
        if (!apply) return new Resolved(profile, evidence);
        HairProfile effective = profile == null
                ? HairProfile.create(userId, null, null, observed, null, null)
                : profile.replace(profile.hairType(), profile.hairCondition(), observed,
                        profile.hairThickness(), profile.availableCareTimeMinutes());
        return new Resolved(effective, evidence.withApplied(true));
    }
    public java.util.Map<UUID, Double> similarities(PhotoHairEvidence evidence,
            List<com.heddy.domain.recommendation.model.HairstyleCandidate> candidates) {
        if (evidence.hairShape().isEmpty()) return java.util.Map.of();
        var ids = candidates.stream().map(com.heddy.domain.recommendation.model.HairstyleCandidate::thumbnailFileId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        var observations = new java.util.HashMap<UUID, PhotoHairEvidence>();
        for (var file : files.findAllById(ids)) {
            if (ids.contains(file.fileId()) && file.isReady() && file.ownerType() == FileOwnerType.SYSTEM
                    && file.purpose() == com.heddy.domain.file.model.FilePurpose.HAIRSTYLE_THUMBNAIL) {
                observations.put(file.fileId(), analyzer.analyze(file));
            }
        }
        var result = new java.util.TreeMap<UUID, Double>();
        for (var candidate : candidates) {
            var observation = observations.get(candidate.thumbnailFileId());
            if (observation == null || !java.util.Objects.equals(evidence.modelVersion(), observation.modelVersion())
                    || observation.hairShape().isEmpty()) continue;
            Double similarity = com.heddy.domain.recommendation.service.HairShapeSimilarity.compare(
                    evidence.hairShape(), observation.hairShape());
            if (similarity != null) result.put(candidate.hairstyleId(), similarity);
        }
        return java.util.Map.copyOf(result);
    }

    public record Resolved(HairProfile profile, PhotoHairEvidence evidence) { }
}
