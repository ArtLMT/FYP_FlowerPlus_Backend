-- V7__create_material_table.sql

CREATE TABLE material (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL,
    selling_price DECIMAL(12, 0) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID REFERENCES user_account(id) ON DELETE SET NULL,
    updated_by UUID REFERENCES user_account(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX idx_material_unique_name ON material (LOWER(TRIM(name)));
