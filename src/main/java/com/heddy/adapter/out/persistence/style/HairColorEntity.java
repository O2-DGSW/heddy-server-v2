package com.heddy.adapter.out.persistence.style;

import com.heddy.domain.style.model.HairColor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "hair_colors")
class HairColorEntity {

    @Id
    @Column(name = "color_id", nullable = false)
    private UUID colorId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "hex_code", nullable = false, length = 7)
    private String hexCode;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    protected HairColorEntity() {
    }

    HairColor toDomain() {
        return new HairColor(colorId, code, name, hexCode, sortOrder);
    }
}
