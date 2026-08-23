package com.heddy.adapter.out.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface ConsentHistoryJpaRepository extends JpaRepository<ConsentHistoryEntity, UUID> {

    @Query(value = """
            SELECT DISTINCT ON (consent_type)
                   consent_id, user_id, consent_type, granted,
                   policy_version, source, changed_at, change_sequence
            FROM consent_history
            WHERE user_id = :userId
            ORDER BY consent_type, change_sequence DESC
            """, nativeQuery = true)
    List<ConsentHistoryEntity> findLatestByUserId(@Param("userId") UUID userId);
}
