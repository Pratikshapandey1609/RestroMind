package com.restromind.app.notification.dto;

public record PreferenceRequest(boolean pushEnabled, boolean emailEnabled, boolean inAppEnabled) {}
