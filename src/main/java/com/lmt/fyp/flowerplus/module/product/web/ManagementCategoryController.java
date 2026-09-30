package com.lmt.fyp.flowerplus.module.product.web;

import com.lmt.fyp.flowerplus.module.product.dto.CategoryVersionRequest;
import com.lmt.fyp.flowerplus.module.product.dto.CreateCategoryRequest;
import com.lmt.fyp.flowerplus.module.product.dto.ManagementCategoryResponse;
import com.lmt.fyp.flowerplus.module.product.dto.RenameCategoryRequest;
import com.lmt.fyp.flowerplus.module.product.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/manage/categories")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class ManagementCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<ManagementCategoryResponse> listCategories() {
        return categoryService.listManagementCategories();
    }

    @PostMapping
    public ResponseEntity<ManagementCategoryResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        ManagementCategoryResponse response = categoryService.createCategory(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ManagementCategoryResponse getCategory(@PathVariable UUID id) {
        return categoryService.getManagementCategory(id);
    }

    @PutMapping("/{id}")
    public ManagementCategoryResponse renameCategory(
            @PathVariable UUID id,
            @Valid @RequestBody RenameCategoryRequest request) {
        return categoryService.renameCategory(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryVersionRequest request) {
        categoryService.deleteCategory(id, request.version());
        return ResponseEntity.noContent().build();
    }
}
