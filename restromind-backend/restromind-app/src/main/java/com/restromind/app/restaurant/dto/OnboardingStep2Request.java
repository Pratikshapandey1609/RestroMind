package com.restromind.app.restaurant.dto;

import com.restromind.app.restaurant.entity.CuisineType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OnboardingStep2Request(
    @NotNull CuisineType cuisineType,
    @Size(max = 1000) String description
) {}
