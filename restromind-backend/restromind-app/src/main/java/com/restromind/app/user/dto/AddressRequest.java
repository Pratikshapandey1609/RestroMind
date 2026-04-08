package com.restromind.app.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
    @Size(max = 100) String label,
    @NotBlank String address,
    String city,
    boolean isDefault
) {}
