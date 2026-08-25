package com.heddy.domain.treatment.port.out;

import com.heddy.domain.treatment.model.TreatmentPhoto;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.model.TreatmentRecordFilter;
import com.heddy.domain.treatment.model.TreatmentRecordPage;

import java.util.Optional;
import java.util.UUID;

public interface TreatmentRecordRepositoryPort {

    /** 기록과 그에 첨부된 사진들을 한 번에 저장한다. */
    TreatmentRecord insert(TreatmentRecord record);

    /**
     * 사진 한 장을 단건으로 저장한다.
     *
     * <p>최대 장수 검증은 {@link TreatmentRecord#attachPhoto} 가 도메인에서 이미 했다는
     * 전제다. 이 메서드는 그 결과를 영속화할 뿐 다시 세지 않는다.
     */
    TreatmentPhoto insertPhoto(TreatmentPhoto photo);

    /** 사진 유형과 표시 순서를 저장한다. */
    Optional<TreatmentPhoto> updatePhoto(TreatmentPhoto photo);

    /** 사진 연결 한 건을 삭제한다. */
    boolean deletePhoto(UUID photoId);

    /**
     * 소유자 조건까지 걸어 기록을 사진(생성 시각순)과 함께 조회한다.
     *
     * <p>소유권을 메모리에서 걸러내면 남의 기록도 사진까지 읽은 뒤에야 404 가 되어, 응답 시간과
     * 질의 횟수로 존재 여부가 새어 나간다. 그래서 포트 자체가 소유자 조건을 받는다.
     */
    Optional<TreatmentRecord> findByIdAndUserId(UUID recordId, UUID userId);

    /** 사진 추가 상한을 직렬화하기 위해 기록 행을 쓰기 잠금으로 조회한다. */
    Optional<TreatmentRecord> findByIdForUpdate(UUID recordId);

    /** 소유자 조건을 항상 포함해 필터·페이지 조건에 맞는 기록을 조회한다. */
    TreatmentRecordPage findPage(TreatmentRecordFilter filter);

    /** 존재하는 기록의 수정 가능한 필드를 저장한다. */
    Optional<TreatmentRecord> update(TreatmentRecord record);

    /** 하드 삭제한다. 연결 사진 행은 데이터베이스 CASCADE가 함께 삭제한다. */
    boolean deleteById(UUID recordId);
}
