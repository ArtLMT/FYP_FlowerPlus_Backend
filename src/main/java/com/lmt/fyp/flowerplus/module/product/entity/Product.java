package com.lmt.fyp.flowerplus.module.product.entity;

import com.lmt.fyp.flowerplus.common.entity.AuditableEntity;
import com.lmt.fyp.flowerplus.common.util.StringNormalizer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends AuditableEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 5000)
    private String description;

    @Column(name = "price", precision = 12, scale = 0)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 50)
    private ProductType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProductStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToMany
    @JoinTable(
            name = "product_category",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductRecipe> recipe = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<ProductImage> images = new ArrayList<>();

    public Product(String name, String description, BigDecimal basePrice, ProductType type) {
        this.name = StringNormalizer.strip(name);
        this.description = description;
        this.basePrice = basePrice;
        this.type = type;
        this.status = ProductStatus.DRAFT;
    }

    public void addCategory(Category category) {
        categories.add(category);
    }

    public void addRecipeLine(ProductRecipe recipeLine) {
        recipe.add(recipeLine);
    }

    public void addImage(String storageKey) {
        images.add(new ProductImage(this, storageKey, images.size()));
        setUpdatedAt(Instant.now());
    }

    public void updateDraftDetails(
            String name,
            String description,
            BigDecimal basePrice,
            Set<Category> categories,
            List<RecipeLineUpdate> requestedRecipe) {
        this.name = StringNormalizer.strip(name);
        this.description = description;
        this.basePrice = basePrice;

        this.categories.clear();
        this.categories.addAll(categories);

        Map<UUID, ProductRecipe> existingLines = new HashMap<>();
        for (ProductRecipe line : recipe) {
            existingLines.put(line.getMaterial().getId(), line);
        }
        Set<UUID> retainedMaterialIds = new HashSet<>();
        for (RecipeLineUpdate requestedLine : requestedRecipe) {
            retainedMaterialIds.add(requestedLine.material().getId());
            ProductRecipe existing = existingLines.get(requestedLine.material().getId());
            if (existing == null) {
                recipe.add(new ProductRecipe(this, requestedLine.material(), requestedLine.quantityRequired()));
            } else {
                existing.changeQuantity(requestedLine.quantityRequired());
            }
        }
        recipe.removeIf(line -> !retainedMaterialIds.contains(line.getMaterial().getId()));

        // Association-only edits must still advance the aggregate's version and audit fields.
        setUpdatedAt(Instant.now());
    }

    public record RecipeLineUpdate(com.lmt.fyp.flowerplus.module.material.entity.Material material,
                                   BigDecimal quantityRequired) {
    }
}
