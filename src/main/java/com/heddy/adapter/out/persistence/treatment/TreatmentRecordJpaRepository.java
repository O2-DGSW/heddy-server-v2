package com.heddy.adapter.out.persistence.treatment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface TreatmentRecordJpaRepository extends JpaRepository<TreatmentRecordEntity, UUID> {

    Optional<TreatmentRecordEntity> findByRecordIdAndUserId(UUID recordId, UUID userId);

    long deleteByRecordId(UUID recordId);

    @Query(value = """
            SELECT tr.*
            FROM treatment_records tr
            WHERE tr.user_id = :userId
              AND (CAST(:serviceTypesJson AS jsonb) IS NULL
                   OR tr.service_types @> CAST(:serviceTypesJson AS jsonb))
              AND (CAST(:designerName AS varchar) IS NULL
                   OR tr.designer_name = CAST(:designerName AS varchar))
              AND (CAST(:salonName AS varchar) IS NULL
                   OR tr.salon_name = CAST(:salonName AS varchar))
              AND (CAST(:from AS timestamptz) IS NULL
                   OR tr.performed_at >= CAST(:from AS timestamptz))
              AND (CAST(:to AS timestamptz) IS NULL
                   OR tr.performed_at <= CAST(:to AS timestamptz))
            ORDER BY
              CASE WHEN :ascending = true THEN tr.performed_at END ASC,
              CASE WHEN :ascending = true THEN tr.record_id END ASC,
              CASE WHEN :ascending = false THEN tr.performed_at END DESC,
              CASE WHEN :ascending = false THEN tr.record_id END DESC
            """, countQuery = """
            SELECT count(*)
            FROM treatment_records tr
            WHERE tr.user_id = :userId
              AND (CAST(:serviceTypesJson AS jsonb) IS NULL
                   OR tr.service_types @> CAST(:serviceTypesJson AS jsonb))
              AND (CAST(:designerName AS varchar) IS NULL
                   OR tr.designer_name = CAST(:designerName AS varchar))
              AND (CAST(:salonName AS varchar) IS NULL
                   OR tr.salon_name = CAST(:salonName AS varchar))
              AND (CAST(:from AS timestamptz) IS NULL
                   OR tr.performed_at >= CAST(:from AS timestamptz))
              AND (CAST(:to AS timestamptz) IS NULL
                   OR tr.performed_at <= CAST(:to AS timestamptz))
            """, nativeQuery = true)
    Page<TreatmentRecordEntity> findPage(
            @Param("userId") UUID userId,
            @Param("serviceTypesJson") String serviceTypesJson,
            @Param("designerName") String designerName,
            @Param("salonName") String salonName,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("ascending") boolean ascending,
            Pageable pageable
    );
}
