package com.restromind.app.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
    @NotNull Long dishId,
    @Min(1) int quantity
) {}
