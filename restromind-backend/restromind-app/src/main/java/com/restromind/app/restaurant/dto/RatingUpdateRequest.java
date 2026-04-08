package com.restromind.app.restaurant.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RatingUpdateRequest(
    @NotNull @DecimalMin("0.00") @DecimalMax("5.00") BigDecimal averageRating
) {}
