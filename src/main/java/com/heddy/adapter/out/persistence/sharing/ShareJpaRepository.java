package com.heddy.adapter.out.persistence.sharing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

interface ShareJpaRepository extends JpaRepository<ShareEntity, UUID> {

    Optional<ShareEntity> findByShareIdAndUserId(UUID shareId, UUID userId);

    /**
     * 후보 스타일을 가리키는 공유 연결만 끊는다. 공유 자체는 남기고 사라진 내용만 빠진다.
     * 조인 테이블을 직접 지우는 이유는 이 연결이 @ElementCollection 이라 엔티티가 없어서다.
     */
    @Modifying
    @Query(value = "DELETE FROM share_saved_styles WHERE saved_style_id = :savedStyleId",
            nativeQuery = true)
    void deleteSavedStyleLinks(@Param("savedStyleId") UUID savedStyleId);

    Optional<ShareEntity> findByTokenHash(String tokenHash);

    Page<ShareEntity> findByUserId(UUID userId, Pageable pageable);

    Page<ShareEntity> findByUserIdAndStatus(UUID userId, String status, Pageable pageable);

    /** ACTIVE 필터 전용. 상태가 ACTIVE 라도 만료 시각이 지났으면 더 이상 열리지 않는다. */
    Page<ShareEntity> findByUserIdAndStatusAndExpiresAtAfter(
            UUID userId, String status, Instant now, Pageable pageable);

    void deleteAllByUserId(UUID userId);

    /**
     * 공유 중인 기록 ID 만 골라낸다. 공유 엔티티를 통째로 읽지 않고 조인 행의 record_id 만
     * 뽑는다 — 목록 배지에 필요한 건 "있다/없다" 뿐이라 항목·후보까지 끌고 올 이유가 없다.
     */
    @Query("""
            SELECT recordId FROM ShareEntity share
            JOIN share.recordIds recordId
            WHERE share.userId = :ownerId
              AND share.status = :activeStatus
              AND share.expiresAt > :now
              AND recordId IN :recordIds
            """)
    Set<UUID> findSharedRecordIds(
            @Param("ownerId") UUID ownerId,
            @Param("recordIds") Collection<UUID> recordIds,
            @Param("activeStatus") String activeStatus,
            @Param("now") Instant now);
}
