-- V1__init_schema.sql
-- Initial schema setup for FlowerPlus matching Java entities using UUID primary keys

-- 1. User Account
CREATE TABLE user_account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 2. User Profile
CREATE TABLE user_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    avatar VARCHAR(512)
);

-- 3. Address
CREATE TABLE address (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    receiver_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address TEXT NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 4. Penalty Point Account
CREATE TABLE penalty_point_account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    current_points INT NOT NULL DEFAULT 0
);

-- 5. Penalty Point Transaction
CREATE TABLE penalty_point_transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES penalty_point_account(id) ON DELETE CASCADE,
    point_change INT NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 6. Notification
CREATE TABLE notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    related_entity_type VARCHAR(50),
    related_entity_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 7. Category
CREATE TABLE category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL
);

-- 8. Product
CREATE TABLE product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 9. Product Category (Join Table)
CREATE TABLE product_category (
    product_id UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES category(id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, category_id)
);

-- 10. Product Image
CREATE TABLE product_image (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    image_url VARCHAR(512) NOT NULL,
    display_order INT NOT NULL DEFAULT 0
);

-- 11. Voucher
CREATE TABLE voucher (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) UNIQUE NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    discount_value DECIMAL(12, 2) NOT NULL,
    minimum_order_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    active_from TIMESTAMP WITH TIME ZONE NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    max_redemptions INT,
    max_redemptions_per_user INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 12. Customer Voucher (Join Table)
CREATE TABLE customer_voucher (
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    voucher_id UUID NOT NULL REFERENCES voucher(id) ON DELETE CASCADE,
    redeemed BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (user_id, voucher_id)
);

-- 13. Inventory Item
CREATE TABLE inventory_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    unit_cost DECIMAL(12, 2) NOT NULL,
    reorder_level INT NOT NULL DEFAULT 0
);

-- 14. Product Recipe
CREATE TABLE product_recipe (
    product_id UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    inventory_item_id UUID NOT NULL REFERENCES inventory_item(id) ON DELETE CASCADE,
    quantity_required INT NOT NULL DEFAULT 1,
    PRIMARY KEY (product_id, inventory_item_id)
);

-- 15. Inventory Transaction
CREATE TABLE inventory_transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_item_id UUID NOT NULL REFERENCES inventory_item(id) ON DELETE CASCADE,
    quantity_change INT NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 16. Orders
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES user_account(id),
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    shipping_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    delivery_address TEXT NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    customer_note TEXT,
    payment_method VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE,
    delivery_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 17. Order Details
CREATE TABLE order_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES product(id) ON DELETE SET NULL,
    item_type VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    unit_cost DECIMAL(12, 2) NOT NULL,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT check_item_type CHECK (
        (item_type = 'PRODUCT' AND product_id IS NOT NULL) OR 
        (item_type = 'CUSTOM' AND product_id IS NULL)
    )
);

-- 18. Order Inventory Allocation
CREATE TABLE order_inventory_allocation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    inventory_item_id UUID NOT NULL REFERENCES inventory_item(id) ON DELETE CASCADE,
    quantity_deducted INT NOT NULL,
    allocated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID REFERENCES user_account(id) ON DELETE SET NULL
);

-- 19. Custom Order Detail
CREATE TABLE custom_order_detail (
    order_detail_id UUID PRIMARY KEY REFERENCES order_details(id) ON DELETE CASCADE,
    arrangement_note TEXT,
    labour_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00
);

-- 20. Custom Order Material
CREATE TABLE custom_order_material (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    custom_order_detail_id UUID NOT NULL REFERENCES custom_order_detail(order_detail_id) ON DELETE CASCADE,
    inventory_item_id UUID NOT NULL REFERENCES inventory_item(id) ON DELETE CASCADE,
    quantity INT NOT NULL
);

-- 21. Payment
CREATE TABLE payment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    amount DECIMAL(12, 2) NOT NULL,
    method VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE
);

-- 22. Financial Transaction
CREATE TABLE financial_transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 23. Order Status History
CREATE TABLE order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by UUID REFERENCES user_account(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 24. Conversation
CREATE TABLE conversation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    is_ai_handled BOOLEAN NOT NULL DEFAULT false,
    needs_escalation BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    archived_at TIMESTAMP WITH TIME ZONE,
    last_message_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 25. Message
CREATE TABLE message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    sender_id UUID REFERENCES user_account(id) ON DELETE SET NULL,
    sender_type VARCHAR(50) NOT NULL,
    sender_name VARCHAR(255),
    content TEXT NOT NULL,
    is_ai_generated BOOLEAN NOT NULL DEFAULT false,
    ai_confidence DECIMAL(3, 2),
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 26. FAQ
CREATE TABLE faq (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    keywords TEXT,
    content_chunk TEXT,
    chunk_index INT,
    source VARCHAR(100) NOT NULL DEFAULT 'FAQ',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 27. Refresh Token
CREATE TABLE refresh_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL
);
