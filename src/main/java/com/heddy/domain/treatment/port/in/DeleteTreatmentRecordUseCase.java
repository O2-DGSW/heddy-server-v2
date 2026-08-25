package com.heddy.domain.treatment.port.in;

import java.util.UUID;

/** 내 시술기록과 연결 사진을 삭제하고 파일을 회수 대상으로 전이한다. */
public interface DeleteTreatmentRecordUseCase {

    void delete(Command command);

    record Command(UUID requesterId, UUID recordId) {
    }
}
