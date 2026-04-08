package com.restromind.app.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateDishRequest(
    @NotBlank @Size(max = 255) String name,
    String description,
    @NotNull @DecimalMin("0.00") BigDecimal price,
    String imageUrl,
    String allergens
) {}
