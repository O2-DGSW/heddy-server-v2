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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public Optional<TreatmentPhoto> updatePhoto(TreatmentPhoto photo) {
        return photoRepository.findById(photo.photoId()).map(entity -> {
            entity.update(photo);
            return photoRepository.saveAndFlush(entity).toDomain();
        });
    }

    @Override
    public boolean deletePhoto(UUID photoId) {
        boolean deleted = photoRepository.deleteByPhotoId(photoId) == 1;
        if (deleted) {
            photoRepository.flush();
        }
        return deleted;
    }

    @Override
    public Optional<TreatmentRecord> findByIdAndUserId(UUID recordId, UUID userId) {
        // 소유자 조건을 질의에 실어 보낸다. 남의 기록이면 사진을 읽기 전에 빈 값이 된다.
        return recordRepository.findByRecordIdAndUserId(recordId, userId)
                .map(entity -> entity.toDomain(photosOf(recordId)));
    }

    @Override
    public Optional<TreatmentRecord> findByIdForUpdate(UUID recordId) {
        return recordRepository.findByIdForUpdate(recordId)
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
        Map<UUID, List<TreatmentPhoto>> photosByRecord = photosOf(result.getContent());
        List<TreatmentRecord> records = result.getContent().stream()
                .map(entity -> entity.toDomain(
                        photosByRecord.getOrDefault(entity.recordId(), List.of())))
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
        boolean deleted = recordRepository.deleteByRecordId(recordId) == 1;
        if (deleted) {
            recordRepository.flush();
        }
        return deleted;
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        recordRepository.deleteAllByUserId(userId);
        recordRepository.flush();
    }

    private List<TreatmentPhoto> photosOf(UUID recordId) {
        return photoRepository.findByRecordIdOrderBySortOrderAscCreatedAtAscPhotoIdAsc(recordId).stream()
                .map(TreatmentPhotoEntity::toDomain)
                .toList();
    }

    /**
     * 페이지에 속한 기록들의 사진을 질의 한 번으로 읽어 기록별로 모은다. 기록마다 사진을 다시
     * 읽으면 페이지 크기만큼 질의가 늘어나므로(#66) IN 조회 하나로 고정한다. 개별 기록과 같은
     * 정렬 규칙이므로 기록별 상대 순서는 유지된다.
     */
    private Map<UUID, List<TreatmentPhoto>> photosOf(List<TreatmentRecordEntity> records) {
        if (records.isEmpty()) {
            return Map.of();
        }
        List<UUID> recordIds = records.stream()
                .map(TreatmentRecordEntity::recordId)
                .toList();
        Map<UUID, List<TreatmentPhoto>> grouped = new LinkedHashMap<>();
        for (TreatmentPhotoEntity entity : photoRepository
                .findByRecordIdInOrderBySortOrderAscCreatedAtAscPhotoIdAsc(recordIds)) {
            grouped.computeIfAbsent(entity.recordId(), ignored -> new ArrayList<>())
                    .add(entity.toDomain());
        }
        return grouped;
    }
}
