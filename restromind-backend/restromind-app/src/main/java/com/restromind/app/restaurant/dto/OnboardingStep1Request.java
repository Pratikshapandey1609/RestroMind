package com.restromind.app.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OnboardingStep1Request(
    @NotBlank @Size(max = 255) String name,
    String logoUrl
) {}
