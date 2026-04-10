package com.restromind.app.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderPrintResponse(
    Long orderId,
    String restaurantName,
    String status,
    Instant createdAt,
    String deliveryAddress,
    String specialInstructions,
    List<OrderItemResponse> items,
    BigDecimal subtotal,
    BigDecimal deliveryFee,
    BigDecimal grandTotal
) {}
