CREATE SCHEMA IF NOT EXISTS restromind_menu;

CREATE TABLE restromind_menu.categories (
    id            BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT       NOT NULL,
    name          VARCHAR(255) NOT NULL,
    sort_index    INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (restaurant_id, name)
);

CREATE TABLE restromind_menu.dishes (
    id            BIGSERIAL PRIMARY KEY,
    category_id   BIGINT         NOT NULL REFERENCES restromind_menu.categories(id),
    restaurant_id BIGINT         NOT NULL,
    name          VARCHAR(255)   NOT NULL,
    description   TEXT,
    price         NUMERIC(10,2)  NOT NULL CHECK (price >= 0),
    image_url     VARCHAR(500),
    allergens     TEXT,
    is_available  BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dishes_restaurant ON restromind_menu.dishes(restaurant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_dishes_category   ON restromind_menu.dishes(category_id)   WHERE deleted_at IS NULL;
