package com.restromind.app.restaurant.dto;

import java.math.BigDecimal;
import java.util.List;

public record RestaurantProfileResponse(
    Long id, String name, String logoUrl, String description, String cuisineType,
    String addressLine1, String addressLine2, String city, String state,
    String postalCode, String country, String phone, String status,
    Integer estimatedDeliveryTime, BigDecimal averageRating, Integer onboardingStep,
    List<OperatingHoursResponse> operatingHours
) {}
