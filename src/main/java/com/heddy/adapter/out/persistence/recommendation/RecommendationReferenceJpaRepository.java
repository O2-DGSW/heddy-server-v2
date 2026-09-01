package com.heddy.adapter.out.persistence.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

interface RecommendationReferenceJpaRepository extends JpaRepository<
        RecommendationReferenceEntity, RecommendationReferenceEntity.Key> { }
