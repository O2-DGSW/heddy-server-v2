package com.heddy.domain.treatment.port.out;

import com.heddy.domain.treatment.model.TreatmentPhoto;
import com.heddy.domain.treatment.model.TreatmentRecord;

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

    /** 기록을 사진(생성 시각순)과 함께 조회한다. */
    Optional<TreatmentRecord> findById(UUID recordId);
}
