package com.restromind.app.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    Long userId,
    Long restaurantId,
    String status,
    BigDecimal subtotal,
    BigDecimal deliveryFee,
    BigDecimal grandTotal,
    String deliveryAddress,
    String specialInstructions,
    String cancellationReason,
    Instant createdAt,
    List<OrderItemResponse> items,
    List<StatusHistoryResponse> statusHistory
) {}
