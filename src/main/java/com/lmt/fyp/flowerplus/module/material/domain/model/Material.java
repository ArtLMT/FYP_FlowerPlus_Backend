package com.lmt.fyp.flowerplus.module.material.domain.model;

import com.lmt.fyp.flowerplus.common.MaterialStatus;
import com.lmt.fyp.flowerplus.common.MaterialType;
import com.lmt.fyp.flowerplus.common.UnitOfMeasure;
import com.lmt.fyp.flowerplus.module.material.domain.exception.InvalidMaterialException;
import com.lmt.fyp.flowerplus.module.material.domain.exception.MaterialNotActiveException;
import lombok.Getter;

import java.time.Duration;
import java.util.UUID;

@Getter
public class Material {

    private static final int MAX_NAME_LENGTH = 255;

    private final UUID id;
    private String name;                    // BR-MAT-09: editable
    private final MaterialType type;        // BR-MAT-01, 02
    private final UnitOfMeasure unitOfMeasure;  // BR-MAT-05, 11: immutable
    private final boolean perishable;       // BR-MAT-10, 11: immutable
    private final Duration shelfLife;       // non-null if and only if perishable
    private MaterialStatus status;          // BR-MAT-06, 12

    private Material(UUID id, String name, MaterialType type, UnitOfMeasure unitOfMeasure,
                     boolean perishable, Duration shelfLife, MaterialStatus status) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.unitOfMeasure = unitOfMeasure;
        this.perishable = perishable;
        this.shelfLife = shelfLife;
        this.status = status;
    }

    public static Material create(String name, MaterialType type, UnitOfMeasure unitOfMeasure,
                                  boolean perishable, Duration shelfLife) {
        return new Material(
                UUID.randomUUID(),
                validName(name),
                required(type, "Material type"),
                required(unitOfMeasure, "Unit of measure"),
                perishable,
                validShelfLife(perishable, shelfLife),
                MaterialStatus.ACTIVE);
    }

    /** BR-MAT-06: recipes and batch receipts call this before accepting a material. */
    public void assertUsableInRecipe() {
        if (status != MaterialStatus.ACTIVE) {
            throw new MaterialNotActiveException("Material " + id + " is not active");
        }
    }

    /** BR-MAT-07: a deactivated material contributes zero available stock. */
    public boolean isActive() {
        return status == MaterialStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = MaterialStatus.INACTIVE;
    }

    /** BR-MAT-12 */
    public void reactivate() {
        this.status = MaterialStatus.ACTIVE;
    }

    /** BR-MAT-09: name is the only editable attribute. */
    public void rename(String newName) {
        this.name = validName(newName);
    }

    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidMaterialException("Material name is required");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidMaterialException(
                    "Material name exceeds " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    /** BR-MAT-10: shelf life and perishability must agree — both directions. */
    private static Duration validShelfLife(boolean perishable, Duration shelfLife) {
        if (perishable) {
            if (shelfLife == null || shelfLife.isZero() || shelfLife.isNegative()) {
                throw new InvalidMaterialException(
                        "A perishable material requires a positive shelf life");
            }
            return shelfLife;
        }
        if (shelfLife != null) {
            throw new InvalidMaterialException(
                    "A non-perishable material must not define a shelf life");
        }
        return null;
    }

    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new InvalidMaterialException(field + " is required");
        }
        return value;
    }
}