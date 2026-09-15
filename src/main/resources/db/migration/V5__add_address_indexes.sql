-- The address table ships in V1; V1 created no indexes beyond primary keys.
CREATE INDEX idx_address_user_id ON address (user_id);

-- At most one default address per user. Partial because a plain
-- UNIQUE (user_id, is_default) would also forbid two non-default addresses.
CREATE UNIQUE INDEX ux_address_one_default_per_user
    ON address (user_id) WHERE is_default;
