package com.restromind.app.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(Long dishId, String dishName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}
