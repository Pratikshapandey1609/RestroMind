package com.restromind.app.analytics.dto;

import java.math.BigDecimal;

public record TopDishDto(Long dishId, String dishName, int quantitySold, BigDecimal revenue) {}
