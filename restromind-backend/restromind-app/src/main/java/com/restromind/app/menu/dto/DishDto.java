package com.restromind.app.menu.dto;

import java.math.BigDecimal;

public record DishDto(
    Long id,
    String name,
    String description,
    BigDecimal price,
    String imageUrl,
    String allergens,
    boolean isAvailable
) {}
