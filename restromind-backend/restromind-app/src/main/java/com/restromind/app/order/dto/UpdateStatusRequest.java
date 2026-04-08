package com.restromind.app.order.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStatusRequest(@NotBlank String status, String cancellationReason) {}
