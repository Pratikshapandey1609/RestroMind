CREATE SCHEMA IF NOT EXISTS restromind_analytics;

CREATE TABLE restromind_analytics.analytics_summaries (
    id              BIGSERIAL      PRIMARY KEY,
    restaurant_id   BIGINT         NOT NULL,
    summary_date    DATE           NOT NULL,
    total_revenue   NUMERIC(12,2)  NOT NULL DEFAULT 0,
    total_orders    INT            NOT NULL DEFAULT 0,
    avg_order_value NUMERIC(10,2)  NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    UNIQUE (restaurant_id, summary_date)
);

CREATE TABLE restromind_analytics.top_dishes_cache (
    id            BIGSERIAL      PRIMARY KEY,
    restaurant_id BIGINT         NOT NULL,
    dish_id       BIGINT         NOT NULL,
    dish_name     VARCHAR(255)   NOT NULL,
    quantity_sold INT            NOT NULL,
    revenue       NUMERIC(12,2)  NOT NULL DEFAULT 0,
    period_start  DATE           NOT NULL,
    period_end    DATE           NOT NULL,
    computed_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analytics_restaurant_date
    ON restromind_analytics.analytics_summaries(restaurant_id, summary_date DESC);
CREATE INDEX idx_top_dishes_restaurant
    ON restromind_analytics.top_dishes_cache(restaurant_id, period_start DESC);
