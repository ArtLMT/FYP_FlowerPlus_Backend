DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM product) THEN
        RAISE EXCEPTION 'V11 requires an empty Product table; inspect and explicitly map existing Product data first';
    END IF;
END $$;

ALTER TABLE product
    ALTER COLUMN description TYPE VARCHAR(5000),
    ALTER COLUMN price DROP NOT NULL,
    ALTER COLUMN price TYPE NUMERIC(12, 0),
    ADD COLUMN product_type VARCHAR(50) NOT NULL,
    ADD CONSTRAINT chk_product_name_nonblank CHECK (LENGTH(TRIM(name)) > 0),
    ADD CONSTRAINT chk_product_description_valid
        CHECK (description IS NULL OR (LENGTH(description) <= 5000 AND LENGTH(TRIM(description)) > 0)),
    ADD CONSTRAINT chk_product_price_positive CHECK (price IS NULL OR price > 0),
    ADD CONSTRAINT chk_product_type CHECK (product_type IN ('PRE_ORDER', 'PRE_MADE')),
    ADD CONSTRAINT chk_product_status CHECK (status IN ('DRAFT', 'ACTIVE', 'DEACTIVATED'));

DROP TABLE product_recipe;

CREATE TABLE product_recipe (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    material_id UUID NOT NULL REFERENCES material(id) ON DELETE RESTRICT,
    quantity_required NUMERIC(12, 2) NOT NULL,
    CONSTRAINT uq_product_recipe_product_material UNIQUE (product_id, material_id),
    CONSTRAINT chk_product_recipe_quantity_positive CHECK (quantity_required > 0)
);

CREATE INDEX idx_product_recipe_material_id ON product_recipe (material_id);
