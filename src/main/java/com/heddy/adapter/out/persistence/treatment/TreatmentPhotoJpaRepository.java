package com.heddy.adapter.out.persistence.treatment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface TreatmentPhotoJpaRepository extends JpaRepository<TreatmentPhotoEntity, UUID> {

    /**
     * 같은 마이크로초에 저장된 사진도 순서가 뒤바뀌지 않도록 photoId 로 보조 정렬한다.
     * 생성 시각만으로는 동률이 나올 수 있다.
     */
    List<TreatmentPhotoEntity> findByRecordIdOrderBySortOrderAscCreatedAtAscPhotoIdAsc(UUID recordId);

    boolean existsByFileId(UUID fileId);

    /**
     * 여러 기록의 사진을 정렬 규칙을 유지한 채 질의 한 번으로 읽는다. 시술기록 목록의 페이지
     * 조립이 기록마다 사진을 다시 읽던 N+1(#66)을 이 IN 조회 하나로 끊는다.
     */
    List<TreatmentPhotoEntity> findByRecordIdInOrderBySortOrderAscCreatedAtAscPhotoIdAsc(
            Collection<UUID> recordIds);

    long deleteByPhotoId(UUID photoId);
}
