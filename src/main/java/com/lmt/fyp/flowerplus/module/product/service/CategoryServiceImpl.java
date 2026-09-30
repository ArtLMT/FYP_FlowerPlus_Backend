package com.lmt.fyp.flowerplus.module.product.service;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.dto.ErrorDetails;
import com.lmt.fyp.flowerplus.exception.ApiException;
import com.lmt.fyp.flowerplus.module.product.dto.CategoryResponse;
import com.lmt.fyp.flowerplus.module.product.dto.CreateCategoryRequest;
import com.lmt.fyp.flowerplus.module.product.dto.ManagementCategoryResponse;
import com.lmt.fyp.flowerplus.module.product.dto.RenameCategoryRequest;
import com.lmt.fyp.flowerplus.module.product.entity.Category;
import com.lmt.fyp.flowerplus.module.product.repository.CategoryProductGuard;
import com.lmt.fyp.flowerplus.module.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryProductGuard categoryProductGuard;
    private final AuditorAware<UUID> auditorAware;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listPublicCategories() {
        return categoryRepository.findAllInDisplayOrder().stream().map(CategoryResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagementCategoryResponse> listManagementCategories() {
        return categoryRepository.findAllInDisplayOrder().stream()
                .map(ManagementCategoryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ManagementCategoryResponse getManagementCategory(UUID id) {
        return ManagementCategoryResponse.from(findCategory(id));
    }

    @Override
    @Transactional
    public ManagementCategoryResponse createCategory(CreateCategoryRequest request) {
        Category category = categoryRepository.saveAndFlush(new Category(request.name()));
        return ManagementCategoryResponse.from(category);
    }

    @Override
    @Transactional
    public ManagementCategoryResponse renameCategory(UUID id, RenameCategoryRequest request) {
        Category category = lockCategory(id);
        requireCurrentVersion(category, request.version());

        category.rename(request.name());
        return ManagementCategoryResponse.from(categoryRepository.saveAndFlush(category));
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id, long version) {
        Category category = lockCategory(id);
        requireCurrentVersion(category, version);

        // All Product category writers must lock Category rows before Product rows.
        List<UUID> linkedProductIds = categoryProductGuard.lockProductsForCategory(id);
        List<UUID> orphanedActiveProductIds = categoryProductGuard
                .findActiveProductsThatWouldBeOrphaned(id);
        if (!orphanedActiveProductIds.isEmpty()) {
            throw new ApiException(
                    ErrorCode.PRODUCT_CATEGORY_IN_USE,
                    "Deleting this category would leave Active products without a category",
                    new ErrorDetails.ProductIds(orphanedActiveProductIds));
        }

        UUID actorId = auditorAware.getCurrentAuditor().orElse(null);
        categoryProductGuard.recordCategoryUnlink(linkedProductIds, actorId);
        categoryProductGuard.unlinkCategory(id);
        categoryRepository.delete(category);
        categoryRepository.flush();
    }

    private Category findCategory(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private Category lockCategory(UUID id) {
        return categoryRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private static void requireCurrentVersion(Category category, Long requestedVersion) {
        if (!Objects.equals(category.getVersion(), requestedVersion)) {
            throw new ApiException(ErrorCode.CONCURRENT_MODIFICATION);
        }
    }
}
