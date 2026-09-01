package com.heddy.adapter.out.persistence.hairstyle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface HairstyleRecommendationProfileJpaRepository
        extends JpaRepository<HairstyleRecommendationProfileEntity, UUID> { }
