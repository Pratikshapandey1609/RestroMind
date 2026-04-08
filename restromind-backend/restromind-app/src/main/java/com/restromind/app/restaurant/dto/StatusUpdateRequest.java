package com.restromind.app.restaurant.dto;

import jakarta.validation.constraints.NotBlank;

public record StatusUpdateRequest(@NotBlank String status) {}
