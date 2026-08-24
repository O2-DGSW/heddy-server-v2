package com.heddy.adapter.out.persistence.treatment;

import com.heddy.domain.treatment.model.TreatmentPhoto;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TreatmentPersistenceAdapter implements TreatmentRecordRepositoryPort {

    private final TreatmentRecordJpaRepository recordRepository;
    private final TreatmentPhotoJpaRepository photoRepository;

    @Override
    public TreatmentRecord insert(TreatmentRecord record) {
        TreatmentRecordEntity saved = recordRepository.saveAndFlush(new TreatmentRecordEntity(record));
        List<TreatmentPhoto> photos = record.photos().stream()
                .map(photo -> insertPhoto(photo))
                .toList();
        return saved.toDomain(photos);
    }

    @Override
    public TreatmentPhoto insertPhoto(TreatmentPhoto photo) {
        return photoRepository.saveAndFlush(new TreatmentPhotoEntity(photo)).toDomain();
    }

    @Override
    public Optional<TreatmentRecord> findById(UUID recordId) {
        return recordRepository.findById(recordId)
                .map(entity -> entity.toDomain(photosOf(recordId)));
    }

    private List<TreatmentPhoto> photosOf(UUID recordId) {
        return photoRepository.findByRecordIdOrderByCreatedAtAsc(recordId).stream()
                .map(TreatmentPhotoEntity::toDomain)
                .toList();
    }
}
