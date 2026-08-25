package com.heddy.domain.file.port.in;

import java.util.UUID;

/** POST /uploads/{uploadId}/complete. 스토리지 실물을 검증해 PENDING 을 READY 로 전이한다. */
public interface CompleteUploadUseCase {

    /**
     * 이미 {@code READY} 인 세션에 대한 재요청은 저장된 결과를 다시 돌려주는 멱등 동작으로
     * 정한다. complete 은 응답을 잃은 클라이언트가 재시도하는 요청이므로, 한 번 통과한 완료를
     * 거부하면 정상 처리 건이 실패로 보인다.
     *
     * <p>세션 소유자와 요청자가 다르면 소유자 확인을 먼저 하고 거부한다. 존재 여부를 먼저
     * 알려주면 남의 uploadId 로 세션을 훑을 수 있다.
     */
    CompleteUploadResult complete(CompleteUploadCommand command);
}
