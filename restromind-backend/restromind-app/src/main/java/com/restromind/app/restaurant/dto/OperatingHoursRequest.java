package com.restromind.app.restaurant.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record OperatingHoursRequest(
    @NotNull DayOfWeek dayOfWeek,
    @NotNull LocalTime openTime,
    @NotNull LocalTime closeTime
) {}
