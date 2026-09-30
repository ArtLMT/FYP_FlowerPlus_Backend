package com.lmt.fyp.flowerplus.module.product.web;

import com.lmt.fyp.flowerplus.module.product.dto.CategoryResponse;
import com.lmt.fyp.flowerplus.module.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponse> listCategories() {
        return categoryService.listPublicCategories();
    }
}
