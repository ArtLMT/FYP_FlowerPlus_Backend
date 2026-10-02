package com.lmt.fyp.flowerplus.module.product.entity;

import com.lmt.fyp.flowerplus.module.material.entity.Material;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_recipe")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "quantity_required", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityRequired;

    public ProductRecipe(Product product, Material material, BigDecimal quantityRequired) {
        this.product = product;
        this.material = material;
        this.quantityRequired = quantityRequired;
    }

    public void changeQuantity(BigDecimal quantityRequired) {
        this.quantityRequired = quantityRequired;
    }
}
