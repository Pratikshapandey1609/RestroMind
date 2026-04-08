package com.restromind.app.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

public record OnboardingStep3Request(
    @NotBlank String addressLine1,
    String addressLine2,
    @NotBlank String city,
    @NotBlank String state,
    @NotBlank String postalCode,
    @NotBlank String country,
    String phone,
    BigDecimal latitude,
    BigDecimal longitude,
    Integer estimatedDeliveryTime,
    @NotEmpty List<OperatingHoursRequest> operatingHours
) {}
