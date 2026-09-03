package com.heddy.adapter.out.persistence.style;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface HairColorJpaRepository extends JpaRepository<HairColorEntity, UUID> {

    List<HairColorEntity> findAllByActiveTrueOrderBySortOrderAscColorIdAsc();
}
