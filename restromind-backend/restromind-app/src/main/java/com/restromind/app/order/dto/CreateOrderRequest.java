package com.restromind.app.order.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
    @NotNull Long restaurantId,
    @NotEmpty @Valid List<OrderItemRequest> items,
    String deliveryAddress,
    String specialInstructions
) {}
