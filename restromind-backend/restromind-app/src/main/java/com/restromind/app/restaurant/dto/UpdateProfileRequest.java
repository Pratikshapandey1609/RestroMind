package com.restromind.app.restaurant.dto;

import com.restromind.app.restaurant.entity.CuisineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record UpdateProfileRequest(
    @NotBlank @Size(max = 255) String name,
    String logoUrl,
    String description,
    CuisineType cuisineType,
    String addressLine1,
    String addressLine2,
    String city,
    String state,
    String postalCode,
    String country,
    String phone,
    BigDecimal latitude,
    BigDecimal longitude,
    Integer estimatedDeliveryTime,
    List<OperatingHoursRequest> operatingHours
) {}
