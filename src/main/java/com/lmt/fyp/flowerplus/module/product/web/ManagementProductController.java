package com.lmt.fyp.flowerplus.module.product.web;

import com.lmt.fyp.flowerplus.common.dto.PageResponse;
import com.lmt.fyp.flowerplus.module.product.dto.CreateProductRequest;
import com.lmt.fyp.flowerplus.module.product.dto.ManagementProductResponse;
import com.lmt.fyp.flowerplus.module.product.dto.UpdateProductRequest;
import com.lmt.fyp.flowerplus.module.product.service.ProductImageContent;
import com.lmt.fyp.flowerplus.module.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/manage/products")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class ManagementProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ManagementProductResponse> createDraft(
            @Valid @RequestBody CreateProductRequest request) {
        ManagementProductResponse response = productService.createDraft(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public PageResponse<ManagementProductResponse> listManagementProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return productService.listManagementProducts(page, size, search);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ManagementProductResponse> updateDraft(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateDraft(id, request));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ManagementProductResponse> uploadProductImage(
            @PathVariable UUID id,
            @RequestParam Long version,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(201).body(productService.uploadProductImage(id, version, file));
    }

    @GetMapping("/{id}/images/{imageId}/content")
    public ResponseEntity<byte[]> getProductImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId) {
        ProductImageContent image = productService.getManagementProductImage(id, imageId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(image.mediaType()))
                .body(image.bytes());
    }

    @GetMapping("/{id}")
    public ManagementProductResponse getManagementProduct(@PathVariable UUID id) {
        return productService.getManagementProduct(id);
    }
}
