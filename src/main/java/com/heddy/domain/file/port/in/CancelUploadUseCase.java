package com.heddy.domain.file.port.in;

/**
 * DELETE /uploads/{uploadId}. 미완료 업로드 세션을 취소하고 스토리지 객체와 세션 행을 함께 정리한다.
 */
public interface CancelUploadUseCase {

    /**
     * {@code PENDING} 인 세션만 취소한다. READY 는 완료 검증을 통과해 다른 도메인이 참조할 수 있는
     * 상태라 업로드 취소의 대상이 아니고, 이미 DELETED 인 세션은 아무 것도 하지 않고 성공으로 답한다.
     * DELETE 는 원래 멱등 동작이라 응답을 잃고 재시도한 요청까지 거부할 필요가 없다.
     */
    void cancel(CancelUploadCommand command);
}
