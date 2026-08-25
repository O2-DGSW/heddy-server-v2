package com.heddy.adapter.out.persistence.treatment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

interface TreatmentRecordJpaRepository extends JpaRepository<TreatmentRecordEntity, UUID> {

    @Query(value = """
            SELECT tr.*
            FROM treatment_records tr
            WHERE tr.user_id = :userId
              AND (:serviceTypesJson IS NULL
                   OR tr.service_types @> CAST(:serviceTypesJson AS jsonb))
              AND (:designerName IS NULL OR tr.designer_name = :designerName)
              AND (:salonName IS NULL OR tr.salon_name = :salonName)
              AND (:from IS NULL OR tr.performed_at >= :from)
              AND (:to IS NULL OR tr.performed_at <= :to)
            ORDER BY
              CASE WHEN :ascending = true THEN tr.performed_at END ASC,
              CASE WHEN :ascending = true THEN tr.record_id END ASC,
              CASE WHEN :ascending = false THEN tr.performed_at END DESC,
              CASE WHEN :ascending = false THEN tr.record_id END DESC
            """, countQuery = """
            SELECT count(*)
            FROM treatment_records tr
            WHERE tr.user_id = :userId
              AND (:serviceTypesJson IS NULL
                   OR tr.service_types @> CAST(:serviceTypesJson AS jsonb))
              AND (:designerName IS NULL OR tr.designer_name = :designerName)
              AND (:salonName IS NULL OR tr.salon_name = :salonName)
              AND (:from IS NULL OR tr.performed_at >= :from)
              AND (:to IS NULL OR tr.performed_at <= :to)
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
