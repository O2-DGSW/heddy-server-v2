package com.heddy.adapter.out.persistence.account;

import com.heddy.domain.account.model.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ConsentHistoryJpaRepository extends JpaRepository<ConsentHistoryEntity, UUID> {

    @Query(value = """
            SELECT DISTINCT ON (consent_type)
                   consent_id, user_id, consent_type, granted,
                   policy_version, source, changed_at
            FROM consent_history
            WHERE user_id = :userId
            ORDER BY consent_type, changed_at DESC, consent_id DESC
            """, nativeQuery = true)
    List<ConsentHistoryEntity> findLatestByUserId(@Param("userId") UUID userId);

    Optional<ConsentHistoryEntity>
            findFirstByUserIdAndConsentTypeOrderByChangedAtDescConsentIdDesc(
                    UUID userId,
                    ConsentType consentType
            );
}
