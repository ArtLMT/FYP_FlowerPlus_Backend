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
import com.lmt.fyp.flowerplus.module.material.repository.MaterialReferenceGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialReferenceGuard materialReferenceGuard;

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
        Material material = materialRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(ErrorCode.MATERIAL_NOT_FOUND));
        requireCurrentVersion(material, request.version());

        boolean unitOrTypeChanged = material.getUnitOfMeasure() != request.unitOfMeasure()
                || material.getType() != request.type();
        if (unitOrTypeChanged && materialReferenceGuard.isUsedByProductRecipe(id)) {
            throw new ApiException(
                    ErrorCode.MATERIAL_IN_USE,
                    "Material unit and type cannot change while a Product recipe references it");
        }

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
    public MaterialResponse deactivateMaterial(UUID id, Long version) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.MATERIAL_NOT_FOUND));
        requireCurrentVersion(material, version);

        material.deactivate();
        material = materialRepository.saveAndFlush(material);
        return MaterialResponse.from(material);
    }

    @Override
    @Transactional
    public MaterialResponse reactivateMaterial(UUID id, Long version) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.MATERIAL_NOT_FOUND));
        requireCurrentVersion(material, version);

        material.reactivate();
        material = materialRepository.saveAndFlush(material);
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

        String normalizedSearch = StringNormalizer.strip(search);
        if (normalizedSearch != null && !normalizedSearch.isEmpty()) {
            String likePattern = "%" + escapeLike(normalizedSearch.toLowerCase(Locale.ROOT)) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), likePattern, '\\'));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return materialRepository.findAll(spec, pageable).map(MaterialResponse::from);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /* check if the requested version is correct and match the database's material
    if they don't match mean there's a change happened BEFORE that request, prevent this request to override data */
    private static void requireCurrentVersion(Material material, Long requestedVersion) {
        if (!Objects.equals(material.getVersion(), requestedVersion)) {
            throw new ApiException(ErrorCode.CONCURRENT_MODIFICATION);
        }
    }
}
