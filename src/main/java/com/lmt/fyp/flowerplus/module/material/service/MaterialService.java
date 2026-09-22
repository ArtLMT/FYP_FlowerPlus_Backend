package com.lmt.fyp.flowerplus.module.material.service;

import com.lmt.fyp.flowerplus.module.material.dto.CreateMaterialRequest;
import com.lmt.fyp.flowerplus.module.material.dto.MaterialResponse;
import com.lmt.fyp.flowerplus.module.material.dto.UpdateMaterialRequest;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialStatus;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MaterialService {
    MaterialResponse createMaterial(CreateMaterialRequest request);
    MaterialResponse updateMaterial(UUID id, UpdateMaterialRequest request);
    MaterialResponse deactivateMaterial(UUID id);
    MaterialResponse reactivateMaterial(UUID id);
    MaterialResponse getMaterial(UUID id);
    Page<MaterialResponse> listMaterials(Pageable pageable, String search, MaterialType type, MaterialStatus status);
}
