-- Schema inicial do MVP (PRD Secao 8). Apenas as 13 tabelas do MVP; nenhuma tabela de feature futura.

-- TENANCY
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    subdomain VARCHAR(63) NOT NULL,
    name VARCHAR(255) NOT NULL,
    logo_url TEXT,
    whatsapp_number VARCHAR(20),
    delivery_fee NUMERIC(10, 2) NOT NULL DEFAULT 0,
    min_order_value NUMERIC(10, 2) NOT NULL DEFAULT 0,
    is_open BOOLEAN NOT NULL DEFAULT true,
    plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
    plan_expires_at TIMESTAMPTZ,
    order_count_month INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tenants_subdomain UNIQUE (subdomain)
);

-- USUARIO DONO (um por tenant no MVP)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants (id),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_tenant_id ON users (tenant_id);

CREATE TABLE business_hours (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants (id),
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    opens_at TIME,
    closes_at TIME,
    is_closed BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_business_hours_tenant_id ON business_hours (tenant_id);

-- CARDAPIO
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants (id),
    name VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_categories_tenant_id ON categories (tenant_id);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants (id),
    category_id BIGINT NOT NULL REFERENCES categories (id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL,
    image_url TEXT,
    is_available BOOLEAN NOT NULL DEFAULT true,
    position INTEGER NOT NULL DEFAULT 0,
    available_days INTEGER[] -- dias da semana disponiveis (0=Dom...6=Sab); null = todos
);

CREATE INDEX idx_products_tenant_id ON products (tenant_id);
CREATE INDEX idx_products_category_id ON products (category_id);

-- ADICIONAIS
CREATE TABLE additional_groups (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id),
    name VARCHAR(255) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT false,
    selection_type VARCHAR(10) NOT NULL CHECK (selection_type IN ('SINGLE', 'MULTIPLE')),
    min_selections INTEGER NOT NULL DEFAULT 0,
    max_selections INTEGER
);

CREATE INDEX idx_additional_groups_product_id ON additional_groups (product_id);

CREATE TABLE additional_items (
    id BIGSERIAL PRIMARY KEY,
    additional_group_id BIGINT NOT NULL REFERENCES additional_groups (id),
    name VARCHAR(255) NOT NULL,
    additional_price NUMERIC(10, 2) NOT NULL DEFAULT 0,
    is_available BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_additional_items_group_id ON additional_items (additional_group_id);

-- CLIENTES
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants (id),
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_customers_tenant_id ON customers (tenant_id);

-- PEDIDOS
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants (id),
    customer_id BIGINT NOT NULL REFERENCES customers (id),
    status VARCHAR(20) NOT NULL,
    delivery_type VARCHAR(20) NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL,
    delivery_fee NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total NUMERIC(10, 2) NOT NULL,
    address_snapshot TEXT,
    notes TEXT,
    cancellation_reason TEXT,
    idempotency_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_orders_tenant_idempotency_key UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_orders_tenant_id ON orders (tenant_id);
CREATE INDEX idx_orders_customer_id ON orders (customer_id);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders (id),
    product_name_snapshot VARCHAR(255) NOT NULL,
    unit_price_snapshot NUMERIC(10, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE TABLE order_item_additionals (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL REFERENCES order_items (id),
    additional_name_snapshot VARCHAR(255) NOT NULL,
    additional_price_snapshot NUMERIC(10, 2) NOT NULL
);

CREATE INDEX idx_order_item_additionals_order_item_id ON order_item_additionals (order_item_id);

-- Append-only por convencao: apenas INSERT, nunca UPDATE/DELETE (PRD 4.4 / Regra 7).
CREATE TABLE order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders (id),
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    changed_by_user_id BIGINT REFERENCES users (id), -- null quando origem e SYSTEM
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_status_history_order_id ON order_status_history (order_id);

-- ASSINATURA
CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants (id),
    plan VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_period_end TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    external_subscription_id VARCHAR(100) -- preenchido quando Stripe integrar (Fase 2)
);

CREATE INDEX idx_subscriptions_tenant_id ON subscriptions (tenant_id);
