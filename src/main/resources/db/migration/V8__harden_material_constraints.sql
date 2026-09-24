-- V8__harden_material_constraints.sql

ALTER TABLE material
    ADD CONSTRAINT chk_material_selling_price_positive
        CHECK (selling_price > 0),
    ADD CONSTRAINT chk_material_type
        CHECK (type IN ('FLOWER', 'DECORATION')),
    ADD CONSTRAINT chk_material_unit_of_measure
        CHECK (unit_of_measure IN ('STEM', 'PIECE', 'METRE', 'SHEET')),
    ADD CONSTRAINT chk_material_status
        CHECK (status IN ('ACTIVE', 'DEACTIVATED'));
