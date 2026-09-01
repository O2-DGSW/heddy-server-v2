package com.heddy.adapter.out.persistence.hairstyle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "hairstyle_recommendation_profiles")
class HairstyleRecommendationProfileEntity {
    @Id
    @Column(name = "hairstyle_id", nullable = false, updatable = false)
    private UUID hairstyleId;

    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "service_types", columnDefinition = "jsonb")
    private Set<String> serviceTypes = new LinkedHashSet<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "compatible_hair_lengths", columnDefinition = "jsonb")
    private Set<String> compatibleHairLengths = new LinkedHashSet<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "compatible_hair_types", columnDefinition = "jsonb")
    private Set<String> compatibleHairTypes = new LinkedHashSet<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "compatible_hair_thicknesses", columnDefinition = "jsonb")
    private Set<String> compatibleHairThicknesses = new LinkedHashSet<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "compatible_hair_conditions", columnDefinition = "jsonb")
    private Set<String> compatibleHairConditions = new LinkedHashSet<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "contraindicated_hair_conditions", columnDefinition = "jsonb")
    private Set<String> contraindicatedHairConditions = new LinkedHashSet<>();

    @Column(name = "minimum_hair_length", length = 30)
    private String minimumHairLength;
    @Column(name = "estimated_daily_care_minutes", nullable = false)
    private int estimatedDailyCareMinutes;
    @Column(name = "management_difficulty", nullable = false, length = 20)
    private String managementDifficulty;
    @Column(name = "chemical_stress_level", nullable = false, length = 20)
    private String chemicalStressLevel;
    @Column(name = "editorial_priority", nullable = false)
    private int editorialPriority;
    @Column(name = "metadata_version", nullable = false, length = 30)
    private String metadataVersion;

    protected HairstyleRecommendationProfileEntity() { }

    UUID hairstyleId() { return hairstyleId; }
    Set<String> serviceTypes() { return serviceTypes; }
    Set<String> compatibleHairLengths() { return compatibleHairLengths; }
    Set<String> compatibleHairTypes() { return compatibleHairTypes; }
    Set<String> compatibleHairThicknesses() { return compatibleHairThicknesses; }
    Set<String> compatibleHairConditions() { return compatibleHairConditions; }
    Set<String> contraindicatedHairConditions() { return contraindicatedHairConditions; }
    String minimumHairLength() { return minimumHairLength; }
    int estimatedDailyCareMinutes() { return estimatedDailyCareMinutes; }
    String managementDifficulty() { return managementDifficulty; }
    String chemicalStressLevel() { return chemicalStressLevel; }
    int editorialPriority() { return editorialPriority; }
    String metadataVersion() { return metadataVersion; }
}
