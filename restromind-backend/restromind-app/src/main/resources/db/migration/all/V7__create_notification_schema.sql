CREATE SCHEMA IF NOT EXISTS restromind_notification;

CREATE TABLE restromind_notification.notification_preferences (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT    NOT NULL UNIQUE,
    push_enabled   BOOLEAN   NOT NULL DEFAULT TRUE,
    email_enabled  BOOLEAN   NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN   NOT NULL DEFAULT TRUE
);

CREATE TABLE restromind_notification.notification_log (
    id          BIGSERIAL    PRIMARY KEY,
    event_id    UUID         NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL,
    channel     VARCHAR(20)  NOT NULL CHECK (channel IN ('PUSH','EMAIL','IN_APP')),
    title       VARCHAR(255),
    body        TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING','SENT','FAILED')),
    retry_count INT          NOT NULL DEFAULT 0,
    sent_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_log_user
    ON restromind_notification.notification_log(user_id, created_at DESC);
