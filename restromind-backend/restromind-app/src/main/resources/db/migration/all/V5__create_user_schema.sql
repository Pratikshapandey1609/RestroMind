CREATE SCHEMA IF NOT EXISTS restromind_user;

CREATE TABLE restromind_user.user_profiles (
    id            BIGSERIAL    PRIMARY KEY,
    auth_user_id  BIGINT       NOT NULL UNIQUE,
    display_name  VARCHAR(255),
    profile_photo VARCHAR(500),
    phone         VARCHAR(30),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE restromind_user.delivery_addresses (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES restromind_user.user_profiles(id) ON DELETE CASCADE,
    label      VARCHAR(100),
    address    TEXT         NOT NULL,
    city       VARCHAR(100),
    is_default BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Only one default address per user
CREATE UNIQUE INDEX idx_one_default_address
    ON restromind_user.delivery_addresses(user_id)
    WHERE is_default = TRUE;
