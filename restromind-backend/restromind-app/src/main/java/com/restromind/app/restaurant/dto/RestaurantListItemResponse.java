package com.restromind.app.restaurant.dto;

import java.math.BigDecimal;

public record RestaurantListItemResponse(
    Long id, String name, String logoUrl, String cuisineType,
    BigDecimal averageRating, Integer estimatedDeliveryTime, String city
) {}
