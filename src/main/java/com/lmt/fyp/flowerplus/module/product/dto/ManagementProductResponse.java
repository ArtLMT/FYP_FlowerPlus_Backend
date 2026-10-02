package com.lmt.fyp.flowerplus.module.product.dto;

import com.lmt.fyp.flowerplus.module.material.entity.MaterialStatus;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;
import com.lmt.fyp.flowerplus.module.product.entity.Category;
import com.lmt.fyp.flowerplus.module.product.entity.Product;
import com.lmt.fyp.flowerplus.module.product.entity.ProductImage;
import com.lmt.fyp.flowerplus.module.product.entity.ProductRecipe;
import com.lmt.fyp.flowerplus.module.product.entity.ProductStatus;
import com.lmt.fyp.flowerplus.module.product.entity.ProductType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record ManagementProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal basePrice,
        ProductType type,
        ProductStatus status,
        Long version,
        List<RecipeMaterial> recipe,
        List<CategorySummary> categories,
        List<ImageSummary> images,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy
) {

    public static ManagementProductResponse from(Product product) {
        List<RecipeMaterial> recipe = product.getRecipe().stream()
                .sorted(Comparator.comparing(line -> line.getMaterial().getId()))
                .map(RecipeMaterial::from)
                .toList();
        List<CategorySummary> categories = product.getCategories().stream()
                .sorted(Comparator.comparing((Category category) -> category.getName().toLowerCase(Locale.ROOT))
                        .thenComparing(Category::getId))
                .map(CategorySummary::from)
                .toList();
        List<ImageSummary> images = product.getImages().stream()
                .sorted(Comparator.comparingInt(ProductImage::getDisplayOrder).thenComparing(ProductImage::getId))
                .map(image -> new ImageSummary(
                        image.getId(),
                        "/api/manage/products/" + product.getId() + "/images/" + image.getId() + "/content",
                        image.getDisplayOrder()))
                .toList();

        return new ManagementProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getType(),
                product.getStatus(),
                product.getVersion(),
                recipe,
                categories,
                images,
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getCreatedBy(),
                product.getUpdatedBy()
        );
    }

    public record RecipeMaterial(
            UUID materialId,
            String materialName,
            MaterialType materialType,
            UnitOfMeasure materialUnit,
            MaterialStatus materialStatus,
            BigDecimal quantityRequired
    ) {
        private static RecipeMaterial from(ProductRecipe line) {
            return new RecipeMaterial(
                    line.getMaterial().getId(),
                    line.getMaterial().getName(),
                    line.getMaterial().getType(),
                    line.getMaterial().getUnitOfMeasure(),
                    line.getMaterial().getStatus(),
                    line.getQuantityRequired()
            );
        }
    }

    public record CategorySummary(UUID id, String name) {
        private static CategorySummary from(Category category) {
            return new CategorySummary(category.getId(), category.getName());
        }
    }

    public record ImageSummary(UUID id, String contentUrl, int displayOrder) {
    }
}
