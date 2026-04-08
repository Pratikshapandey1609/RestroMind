CREATE SCHEMA IF NOT EXISTS restromind_restaurant;

CREATE TABLE IF NOT EXISTS restromind_restaurant.restaurants (
    id                      BIGSERIAL       PRIMARY KEY,
    owner_id                BIGINT          NOT NULL UNIQUE,
    name                    VARCHAR(255)    NOT NULL,
    logo_url                VARCHAR(500),
    description             TEXT,
    cuisine_type            VARCHAR(50)     NOT NULL DEFAULT 'OTHER'
                                            CHECK (cuisine_type IN (
                                                'ITALIAN','INDIAN','CHINESE','MEXICAN',
                                                'AMERICAN','JAPANESE','MEDITERRANEAN','OTHER'
                                            )),
    address_line1           VARCHAR(255),
    address_line2           VARCHAR(255),
    city                    VARCHAR(100),
    state                   VARCHAR(100),
    postal_code             VARCHAR(20),
    country                 VARCHAR(100),
    phone                   VARCHAR(20),
    latitude                DECIMAL(9,6),
    longitude               DECIMAL(9,6),
    status                  VARCHAR(20)     NOT NULL DEFAULT 'DRAFT'
                                            CHECK (status IN ('DRAFT','ACTIVE','OPEN','CLOSED')),
    estimated_delivery_time INTEGER,
    average_rating          DECIMAL(3,2)    NOT NULL DEFAULT 0.00
                                            CHECK (average_rating >= 0.00 AND average_rating <= 5.00),
    onboarding_step         INTEGER         NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS restromind_restaurant.operating_hours (
    id              BIGSERIAL   PRIMARY KEY,
    restaurant_id   BIGINT      NOT NULL
                                REFERENCES restromind_restaurant.restaurants(id) ON DELETE CASCADE,
    day_of_week     VARCHAR(10) NOT NULL
                                CHECK (day_of_week IN (
                                    'MONDAY','TUESDAY','WEDNESDAY','THURSDAY',
                                    'FRIDAY','SATURDAY','SUNDAY'
                                )),
    open_time       TIME        NOT NULL,
    close_time      TIME        NOT NULL,
    CONSTRAINT chk_hours_order CHECK (close_time > open_time),
    CONSTRAINT uq_restaurant_day UNIQUE (restaurant_id, day_of_week)
);

CREATE INDEX IF NOT EXISTS idx_restaurants_status
    ON restromind_restaurant.restaurants(status);
CREATE INDEX IF NOT EXISTS idx_restaurants_cuisine_type
    ON restromind_restaurant.restaurants(cuisine_type);
CREATE INDEX IF NOT EXISTS idx_restaurants_owner_id
    ON restromind_restaurant.restaurants(owner_id);
CREATE INDEX IF NOT EXISTS idx_operating_hours_restaurant_id
    ON restromind_restaurant.operating_hours(restaurant_id);
