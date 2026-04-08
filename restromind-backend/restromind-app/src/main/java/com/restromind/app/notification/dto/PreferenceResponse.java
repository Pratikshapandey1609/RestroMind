package com.restromind.app.notification.dto;

public record PreferenceResponse(boolean pushEnabled, boolean emailEnabled, boolean inAppEnabled) {}
