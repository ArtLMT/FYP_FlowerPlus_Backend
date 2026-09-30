package com.lmt.fyp.flowerplus.module.product.service;

import com.lmt.fyp.flowerplus.module.product.dto.CategoryResponse;
import com.lmt.fyp.flowerplus.module.product.dto.CreateCategoryRequest;
import com.lmt.fyp.flowerplus.module.product.dto.ManagementCategoryResponse;
import com.lmt.fyp.flowerplus.module.product.dto.RenameCategoryRequest;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    List<CategoryResponse> listPublicCategories();

    List<ManagementCategoryResponse> listManagementCategories();

    ManagementCategoryResponse getManagementCategory(UUID id);

    ManagementCategoryResponse createCategory(CreateCategoryRequest request);

    ManagementCategoryResponse renameCategory(UUID id, RenameCategoryRequest request);

    void deleteCategory(UUID id, long version);
}
