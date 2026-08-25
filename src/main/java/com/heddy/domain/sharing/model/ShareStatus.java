package com.heddy.domain.sharing.model;

/**
 * 공유 링크의 생애 상태. 만료는 상태가 아니라 {@code expires_at} 판정으로 매 요청 검증한다.
 * 철회만 상태로 남는다 — 사용자가 명시적으로 누른 철회는 시간이 흘러도 되돌리면 안 된다.
 */
public enum ShareStatus {
    ACTIVE,
    REVOKED
}
