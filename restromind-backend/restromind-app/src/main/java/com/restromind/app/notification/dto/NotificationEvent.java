package com.restromind.app.notification.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Sent by other services (Order, Restaurant) to trigger a notification.
 * eventId is the idempotency key — same eventId will never create a duplicate.
 */
public record NotificationEvent(
    @NotNull UUID eventId,
    @NotNull Long userId,
    @NotBlank String channel,   // PUSH, EMAIL, IN_APP
    @NotBlank String title,
    String body
) {}
