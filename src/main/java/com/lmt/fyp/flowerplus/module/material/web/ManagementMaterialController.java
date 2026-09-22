package com.lmt.fyp.flowerplus.module.material.web;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.dto.PageResponse;
import com.lmt.fyp.flowerplus.exception.ApiException;
import com.lmt.fyp.flowerplus.module.material.dto.CreateMaterialRequest;
import com.lmt.fyp.flowerplus.module.material.dto.MaterialResponse;
import com.lmt.fyp.flowerplus.module.material.dto.UpdateMaterialRequest;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialStatus;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/manage/materials")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class ManagementMaterialController {

    private final MaterialService materialService;

    @PostMapping
    public ResponseEntity<MaterialResponse> createMaterial(@Valid @RequestBody CreateMaterialRequest request) {
        MaterialResponse response = materialService.createMaterial(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public MaterialResponse updateMaterial(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMaterialRequest request) {
        return materialService.updateMaterial(id, request);
    }

    @PutMapping("/{id}/deactivate")
    public MaterialResponse deactivateMaterial(@PathVariable UUID id) {
        return materialService.deactivateMaterial(id);
    }

    @PutMapping("/{id}/reactivate")
    public MaterialResponse reactivateMaterial(@PathVariable UUID id) {
        return materialService.reactivateMaterial(id);
    }

    @GetMapping("/{id}")
    public MaterialResponse getMaterial(@PathVariable UUID id) {
        return materialService.getMaterial(id);
    }

    @GetMapping
    public PageResponse<MaterialResponse> listMaterials(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MaterialType type,
            @RequestParam(required = false) MaterialStatus status) {

        if (pageable.getPageSize() > 100) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Page size must not exceed 100");
        }

        Page<MaterialResponse> page = materialService.listMaterials(pageable, search, type, status);
        return PageResponse.from(page);
    }
}
