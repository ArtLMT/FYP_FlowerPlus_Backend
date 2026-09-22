package com.lmt.fyp.flowerplus.module.material.entity;

import com.lmt.fyp.flowerplus.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "material")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Material extends AuditableEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MaterialType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UnitOfMeasure unitOfMeasure;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal sellingPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MaterialStatus status;

    public Material(String name, MaterialType type, UnitOfMeasure unitOfMeasure, BigDecimal sellingPrice) {
        this.name = name.trim();
        this.type = type;
        this.unitOfMeasure = unitOfMeasure;
        this.sellingPrice = sellingPrice;
        this.status = MaterialStatus.ACTIVE;
    }

    public void update(String name, MaterialType type, UnitOfMeasure unitOfMeasure, BigDecimal sellingPrice) {
        this.name = name.trim();
        this.type = type;
        this.unitOfMeasure = unitOfMeasure;
        this.sellingPrice = sellingPrice;
    }

    public void deactivate() {
        this.status = MaterialStatus.DEACTIVATED;
    }

    public void reactivate() {
        this.status = MaterialStatus.ACTIVE;
    }
}
