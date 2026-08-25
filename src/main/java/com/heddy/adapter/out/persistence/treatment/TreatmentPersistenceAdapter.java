package com.heddy.adapter.out.persistence.treatment;

import com.heddy.domain.treatment.model.TreatmentPhoto;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.model.TreatmentRecordFilter;
import com.heddy.domain.treatment.model.TreatmentRecordPage;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public Optional<TreatmentRecord> findByIdAndUserId(UUID recordId, UUID userId) {
        // 소유자 조건을 질의에 실어 보낸다. 남의 기록이면 사진을 읽기 전에 빈 값이 된다.
        return recordRepository.findByRecordIdAndUserId(recordId, userId)
                .map(entity -> entity.toDomain(photosOf(recordId)));
    }

    @Override
    public TreatmentRecordPage findPage(TreatmentRecordFilter filter) {
        String serviceTypesJson = filter.serviceType() == null
                ? null : "[\"" + filter.serviceType().name() + "\"]";
        Page<TreatmentRecordEntity> result = recordRepository.findPage(
                filter.userId(), serviceTypesJson, filter.designerName(), filter.salonName(),
                filter.from(), filter.to(), filter.ascending(),
                PageRequest.of(filter.page(), filter.size()));
        List<TreatmentRecord> records = result.getContent().stream()
                .map(entity -> entity.toDomain(photosOf(entity.recordId())))
                .toList();
        return new TreatmentRecordPage(records, result.getTotalElements());
    }

    @Override
    public Optional<TreatmentRecord> update(TreatmentRecord record) {
        return recordRepository.findById(record.recordId()).map(entity -> {
            entity.update(record);
            TreatmentRecordEntity saved = recordRepository.saveAndFlush(entity);
            return saved.toDomain(photosOf(saved.recordId()));
        });
    }

    @Override
    public boolean deleteById(UUID recordId) {
        return recordRepository.deleteByRecordId(recordId) == 1;
    }

    private List<TreatmentPhoto> photosOf(UUID recordId) {
        return photoRepository.findByRecordIdOrderByCreatedAtAscPhotoIdAsc(recordId).stream()
                .map(TreatmentPhotoEntity::toDomain)
                .toList();
    }
}
