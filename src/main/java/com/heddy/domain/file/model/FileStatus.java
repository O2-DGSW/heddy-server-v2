package com.heddy.domain.file.model;

/**
 * 파일의 수명 주기. {@code PENDING} 은 아직 오브젝트 스토리지에 실물이 있는지 확인되지 않은 상태다.
 *
 * <p>다른 도메인은 {@code READY} 인 파일만 참조할 수 있다. 검증되지 않은 파일이 시술기록이나
 * AR 캡처에 붙으면, 실물이 없거나 선언과 다른 내용인 파일을 서비스가 정상인 것처럼 다루게 된다.
 */
public enum FileStatus {
    PENDING,
    READY,
    DELETED
}
