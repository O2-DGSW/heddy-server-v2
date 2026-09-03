package com.heddy.domain.style.port.out;

import com.heddy.domain.style.model.HairColor;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface HairColorRepositoryPort {

    /** 팔레트에 노출할 활성 색을 정해진 순서로 읽는다. */
    List<HairColor> findAllActive();

    /**
     * 식별자로 한 건을 읽는다. 내려간(active=false) 색도 돌려준다 — 이미 저장된 후보가
     * 그 색을 가리키고 있어, 화면에 이름을 못 그리는 것보다 그대로 보여 주는 편이 낫다.
     */
    Optional<HairColor> findById(UUID colorId);
}
