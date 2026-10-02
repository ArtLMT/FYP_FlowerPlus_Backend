package com.lmt.fyp.flowerplus.module.product.service;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.dto.ErrorDetails;
import com.lmt.fyp.flowerplus.common.dto.PageResponse;
import com.lmt.fyp.flowerplus.common.util.StringNormalizer;
import com.lmt.fyp.flowerplus.exception.ApiException;
import com.lmt.fyp.flowerplus.module.material.entity.Material;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialStatus;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;
import com.lmt.fyp.flowerplus.module.material.repository.MaterialRepository;
import com.lmt.fyp.flowerplus.module.product.dto.CreateProductRecipeLineRequest;
import com.lmt.fyp.flowerplus.module.product.dto.CreateProductRequest;
import com.lmt.fyp.flowerplus.module.product.dto.ManagementProductResponse;
import com.lmt.fyp.flowerplus.module.product.dto.UpdateProductRequest;
import com.lmt.fyp.flowerplus.module.product.entity.Category;
import com.lmt.fyp.flowerplus.module.product.entity.Product;
import com.lmt.fyp.flowerplus.module.product.entity.ProductRecipe;
import com.lmt.fyp.flowerplus.module.product.entity.ProductStatus;
import com.lmt.fyp.flowerplus.module.product.repository.CategoryRepository;
import com.lmt.fyp.flowerplus.module.product.repository.ProductImageRepository;
import com.lmt.fyp.flowerplus.module.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;
    private final ProductImageRepository productImageRepository;
    private final LocalProductImageStorage productImageStorage;

    @Override
    @Transactional
    public ManagementProductResponse createDraft(CreateProductRequest request) {
        Product product = new Product(request.name(), request.description(), request.basePrice(), request.type());

        List<UUID> categoryIds = request.categoryIds();
        List<Category> categories = categoryIds.isEmpty()
                ? List.of()
                : categoryRepository.findAllByIdForUpdate(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "One or more Product categories do not exist");
        }
        categories.forEach(product::addCategory);

        List<CreateProductRecipeLineRequest> recipe = request.recipe();
        Set<UUID> materialIds = new HashSet<>();
        for (int index = 0; index < recipe.size(); index++) {
            UUID materialId = recipe.get(index).materialId();
            if (!materialIds.add(materialId)) {
                throw validationFailure(
                        "recipe[" + index + "].materialId",
                        "Unique",
                        "A material may appear only once in a recipe");
            }
        }

        List<Material> lockedMaterials = materialIds.isEmpty()
                ? List.of()
                : materialRepository.findAllByIdForUpdate(materialIds);
        Map<UUID, Material> materials = lockedMaterials.stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));
        if (materials.size() != materialIds.size()) {
            throw new ApiException(ErrorCode.MATERIAL_NOT_FOUND, "One or more recipe materials do not exist");
        }

        for (int index = 0; index < recipe.size(); index++) {
            CreateProductRecipeLineRequest line = recipe.get(index);
            Material material = materials.get(line.materialId());
            if (material.getStatus() != MaterialStatus.ACTIVE) {
                throw new ApiException(
                        ErrorCode.PRODUCT_RECIPE_MATERIAL_INACTIVE,
                        "A newly added recipe line must reference an Active material");
            }
            if (material.getUnitOfMeasure() != UnitOfMeasure.METRE
                    && hasFractionalPart(line.quantityRequired())) {
                throw validationFailure(
                        "recipe[" + index + "].quantityRequired",
                        "UnitOfMeasure",
                        "Only METRE material quantities may be fractional");
            }
            product.addRecipeLine(new ProductRecipe(product, material, line.quantityRequired()));
        }

        return ManagementProductResponse.from(productRepository.saveAndFlush(product));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ManagementProductResponse> listManagementProducts(int page, int size, String search) {
        if (page < 0) {
            throw validationFailure("page", "Min", "Page must be at least 0");
        }
        if (size < 1 || size > 100) {
            throw validationFailure("size", "Range", "Size must be between 1 and 100");
        }

        Specification<Product> specification = (root, query, cb) -> cb.conjunction();
        String normalizedSearch = StringNormalizer.strip(search);
        if (normalizedSearch != null && !normalizedSearch.isEmpty()) {
            String pattern = "%" + escapeLike(normalizedSearch.toLowerCase(Locale.ROOT)) + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), pattern, '\\'));
        }

        Sort stableSort = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return PageResponse.from(
                productRepository.findAll(specification, PageRequest.of(page, size, stableSort)),
                ManagementProductResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ManagementProductResponse getManagementProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        return ManagementProductResponse.from(product);
    }

    @Override
    @Transactional
    public ManagementProductResponse updateDraft(UUID id, UpdateProductRequest request) {
        List<UUID> categoryIds = request.categoryIds().stream().sorted().toList();
        List<Category> categories = categoryIds.isEmpty()
                ? List.of()
                : categoryRepository.findAllByIdForUpdate(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "One or more Product categories do not exist");
        }

        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!Objects.equals(product.getVersion(), request.version())) {
            throw new ApiException(ErrorCode.CONCURRENT_MODIFICATION);
        }
        if (product.getStatus() != ProductStatus.DRAFT) {
            throw new ApiException(
                    ErrorCode.PRODUCT_INVALID_STATE,
                    "Only Draft products can be edited through this initial update operation");
        }
        if (product.getType() != request.type()) {
            throw new ApiException(ErrorCode.PRODUCT_TYPE_IMMUTABLE);
        }

        Set<UUID> requestedMaterialIds = new java.util.TreeSet<>();
        for (int index = 0; index < request.recipe().size(); index++) {
            UUID materialId = request.recipe().get(index).materialId();
            if (!requestedMaterialIds.add(materialId)) {
                throw validationFailure(
                        "recipe[" + index + "].materialId",
                        "Unique",
                        "A material may appear only once in a recipe");
            }
        }
        List<Material> lockedMaterials = requestedMaterialIds.isEmpty()
                ? List.of()
                : materialRepository.findAllByIdForUpdate(requestedMaterialIds);
        Map<UUID, Material> materials = lockedMaterials.stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));
        if (materials.size() != requestedMaterialIds.size()) {
            throw new ApiException(ErrorCode.MATERIAL_NOT_FOUND, "One or more recipe materials do not exist");
        }

        Set<UUID> existingMaterialIds = product.getRecipe().stream()
                .map(line -> line.getMaterial().getId())
                .collect(Collectors.toSet());
        List<Product.RecipeLineUpdate> recipe = new ArrayList<>();
        for (int index = 0; index < request.recipe().size(); index++) {
            CreateProductRecipeLineRequest line = request.recipe().get(index);
            Material material = materials.get(line.materialId());
            if (!existingMaterialIds.contains(line.materialId())
                    && material.getStatus() != MaterialStatus.ACTIVE) {
                throw new ApiException(
                        ErrorCode.PRODUCT_RECIPE_MATERIAL_INACTIVE,
                        "A newly added recipe line must reference an Active material");
            }
            if (material.getUnitOfMeasure() != UnitOfMeasure.METRE
                    && hasFractionalPart(line.quantityRequired())) {
                throw validationFailure(
                        "recipe[" + index + "].quantityRequired",
                        "UnitOfMeasure",
                        "Only METRE material quantities may be fractional");
            }
            recipe.add(new Product.RecipeLineUpdate(material, line.quantityRequired()));
        }

        product.updateDraftDetails(
                request.name(),
                request.description(),
                request.basePrice(),
                new java.util.LinkedHashSet<>(categories),
                recipe);
        return ManagementProductResponse.from(productRepository.saveAndFlush(product));
    }

    @Override
    @Transactional
    public ManagementProductResponse uploadProductImage(UUID id, Long version, MultipartFile file) {
        // Write the bytes before taking the Product lock; filesystem IO should not
        // hold a database row lock. If the database transaction rolls back, remove
        // the staged file so storage and the product_image row stay in sync.
        String storageKey = productImageStorage.store(id, file);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    productImageStorage.delete(storageKey);
                }
            }
        });

        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!Objects.equals(product.getVersion(), version)) {
            throw new ApiException(ErrorCode.CONCURRENT_MODIFICATION);
        }
        if (product.getImages().size() >= 5) {
            throw new ApiException(ErrorCode.PRODUCT_IMAGE_LIMIT_REACHED);
        }

        product.addImage(storageKey);
        return ManagementProductResponse.from(productRepository.saveAndFlush(product));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductImageContent getManagementProductImage(UUID productId, UUID imageId) {
        var image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        String storageKey = image.getStorageKey();
        return new ProductImageContent(
                productImageStorage.load(storageKey),
                productImageStorage.mediaType(storageKey));
    }

    private static boolean hasFractionalPart(BigDecimal value) {
        return value.stripTrailingZeros().scale() > 0;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static ApiException validationFailure(String field, String rule, String message) {
        return new ApiException(
                ErrorCode.VALIDATION_FAILED,
                message,
                new ErrorDetails.Validation(List.of(new ErrorDetails.FieldViolation(field, rule, message))));
    }
}
