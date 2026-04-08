package com.restromind.app.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(max = 255) String displayName,
    String profilePhoto,
    @Size(max = 30) String phone
) {}
