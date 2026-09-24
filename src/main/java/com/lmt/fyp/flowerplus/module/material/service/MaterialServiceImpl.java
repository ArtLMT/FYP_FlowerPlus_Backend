package com.lmt.fyp.flowerplus.module.material.service;

import com.lmt.fyp.flowerplus.module.material.dto.CreateMaterialRequest;
import com.lmt.fyp.flowerplus.module.material.dto.MaterialResponse;
import com.lmt.fyp.flowerplus.module.material.dto.UpdateMaterialRequest;
import com.lmt.fyp.flowerplus.module.material.entity.Material;
import com.lmt.fyp.flowerplus.common.util.StringNormalizer;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialStatus;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.exception.ApiException;
import com.lmt.fyp.flowerplus.module.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;

    @Override
    @Transactional
    public MaterialResponse createMaterial(CreateMaterialRequest request) {
        String normalizedName = StringNormalizer.strip(request.name());
        if (materialRepository.existsByNormalizedNameIgnoreCase(normalizedName)) {
            throw new ApiException(ErrorCode.MATERIAL_NAME_EXISTS);
        }

        Material material = new Material(
                normalizedName,
                request.type(),
                request.unitOfMeasure(),
                request.sellingPrice()
        );
        material = materialRepository.saveAndFlush(material);
        return MaterialResponse.from(material);
    }

    @Override
    @Transactional
    public MaterialResponse updateMaterial(UUID id, UpdateMaterialRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.MATERIAL_NOT_FOUND));

        String normalizedName = StringNormalizer.strip(request.name());
        if (materialRepository.existsByNormalizedNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new ApiException(ErrorCode.MATERIAL_NAME_EXISTS);
        }

        material.update(normalizedName, request.type(), request.unitOfMeasure(), request.sellingPrice());
        material = materialRepository.saveAndFlush(material);
        return MaterialResponse.from(material);
    }

    @Override
    @Transactional
    public MaterialResponse deactivateMaterial(UUID id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.MATERIAL_NOT_FOUND));

        material.deactivate();
        material = materialRepository.save(material);
        return MaterialResponse.from(material);
    }

    @Override
    @Transactional
    public MaterialResponse reactivateMaterial(UUID id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.MATERIAL_NOT_FOUND));

        material.reactivate();
        material = materialRepository.save(material);
        return MaterialResponse.from(material);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialResponse getMaterial(UUID id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.MATERIAL_NOT_FOUND));
        return MaterialResponse.from(material);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaterialResponse> listMaterials(Pageable pageable, String search, MaterialType type, MaterialStatus status) {
        Specification<Material> spec = (root, query, cb) -> cb.conjunction();

        if (StringUtils.hasText(search)) {
            String likePattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), likePattern));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return materialRepository.findAll(spec, pageable).map(MaterialResponse::from);
    }
}
