CREATE SCHEMA IF NOT EXISTS restromind_order;

CREATE TABLE restromind_order.orders (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT         NOT NULL,
    restaurant_id        BIGINT         NOT NULL,
    status               VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    subtotal             NUMERIC(10,2)  NOT NULL,
    delivery_fee         NUMERIC(10,2)  NOT NULL DEFAULT 0,
    grand_total          NUMERIC(10,2)  NOT NULL,
    delivery_address     TEXT,
    special_instructions TEXT,
    cancellation_reason  TEXT,
    delivered_at         TIMESTAMPTZ,
    cancelled_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_grand_total CHECK (grand_total >= 0),
    CONSTRAINT chk_subtotal    CHECK (subtotal = grand_total - delivery_fee)
);

CREATE TABLE restromind_order.order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT        NOT NULL REFERENCES restromind_order.orders(id) ON DELETE CASCADE,
    dish_id     BIGINT        NOT NULL,
    dish_name   VARCHAR(255)  NOT NULL,
    unit_price  NUMERIC(10,2) NOT NULL,
    quantity    INT           NOT NULL CHECK (quantity > 0),
    line_total  NUMERIC(10,2) NOT NULL,
    CONSTRAINT chk_line_total CHECK (line_total = unit_price * quantity)
);

CREATE TABLE restromind_order.order_status_history (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT      NOT NULL REFERENCES restromind_order.orders(id) ON DELETE CASCADE,
    status     VARCHAR(20) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    changed_by BIGINT
);

CREATE INDEX idx_orders_restaurant ON restromind_order.orders(restaurant_id, created_at DESC);
CREATE INDEX idx_orders_user       ON restromind_order.orders(user_id, created_at DESC);
