package com.lmt.fyp.flowerplus.module.product.service;

import com.lmt.fyp.flowerplus.common.dto.PageResponse;
import com.lmt.fyp.flowerplus.module.product.dto.CreateProductRequest;
import com.lmt.fyp.flowerplus.module.product.dto.ManagementProductResponse;
import com.lmt.fyp.flowerplus.module.product.dto.UpdateProductRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ProductService {

    ManagementProductResponse createDraft(CreateProductRequest request);

    PageResponse<ManagementProductResponse> listManagementProducts(int page, int size, String search);

    ManagementProductResponse getManagementProduct(UUID id);

    ManagementProductResponse updateDraft(UUID id, UpdateProductRequest request);

    ManagementProductResponse uploadProductImage(UUID id, Long version, MultipartFile file);

    ProductImageContent getManagementProductImage(UUID productId, UUID imageId);
}
