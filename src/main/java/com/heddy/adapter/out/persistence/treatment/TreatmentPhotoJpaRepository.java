package com.heddy.adapter.out.persistence.treatment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TreatmentPhotoJpaRepository extends JpaRepository<TreatmentPhotoEntity, UUID> {

    /**
     * 같은 마이크로초에 저장된 사진도 순서가 뒤바뀌지 않도록 photoId 로 보조 정렬한다.
     * 생성 시각만으로는 동률이 나올 수 있다.
     */
    List<TreatmentPhotoEntity> findByRecordIdOrderByCreatedAtAscPhotoIdAsc(UUID recordId);
}
