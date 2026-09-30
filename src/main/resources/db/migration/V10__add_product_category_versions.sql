-- Category management is the first Product slice. Existing Product rows are
-- retained and versioned because category deletion can unlink hidden products.
ALTER TABLE category
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN created_by UUID REFERENCES user_account(id) ON DELETE SET NULL,
    ADD COLUMN updated_by UUID REFERENCES user_account(id) ON DELETE SET NULL,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX idx_category_unique_name ON category (LOWER(TRIM(name)));

ALTER TABLE product
    ADD COLUMN created_by UUID REFERENCES user_account(id) ON DELETE SET NULL,
    ADD COLUMN updated_by UUID REFERENCES user_account(id) ON DELETE SET NULL,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
