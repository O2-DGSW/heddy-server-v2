package com.heddy.domain.file.port.in;

/**
 * 취소된 업로드 세션의 스토리지 객체를 만료 이후 최종 회수한다.
 *
 * <p>취소가 객체를 지워도 그것으로 끝이 아니다. presigned PUT URL 은 세션 만료까지 유효하므로
 * 취소와 겹쳐 전송 중이던 PUT 이나 클라이언트 재시도가 성공하면 객체만 되살아난다. 이때 행은 이미
 * DELETED 라 PENDING·READY 를 훑는 정리 작업의 대상이 아니고, 아무도 가리키지 않는 사용자 사진이
 * 스토리지에 영구히 남는다.
 *
 * <p>그래서 취소는 회수를 확정하지 않고 표시를 비워 둔다. 이 경로가 만료된 뒤에 — URL 로 객체가
 * 다시 생길 수 없게 된 뒤에 — 같은 키를 한 번 더 지우고 회수를 확정한다.
 */
public interface ReclaimUploadObjectsUseCase {

    /**
     * 만료된 취소 세션의 객체를 회수한다.
     *
     * @param limit 한 번에 처리할 최대 세션 수
     * @return 이번 호출에서 회수를 확정한 세션 수
     */
    int reclaimExpired(int limit);
}
