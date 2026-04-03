-- V1__create_auth_schema.sql

CREATE SCHEMA IF NOT EXISTS restromind_auth;

CREATE TABLE IF NOT EXISTS restromind_auth.users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    full_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(10)  NOT NULL CHECK (role IN ('ADMIN','USER')),
    profile_photo VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS restromind_auth.refresh_tokens (
    id         BIGSERIAL   PRIMARY KEY,
    jti        UUID        NOT NULL UNIQUE,
    user_id    BIGINT      NOT NULL REFERENCES restromind_auth.users(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON restromind_auth.refresh_tokens(user_id);
