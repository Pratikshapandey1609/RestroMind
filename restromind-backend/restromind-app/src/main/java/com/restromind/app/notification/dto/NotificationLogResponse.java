package com.restromind.app.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationLogResponse(
    Long id,
    UUID eventId,
    String channel,
    String title,
    String body,
    String status,
    Instant createdAt
) {}
