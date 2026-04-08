package com.restromind.app.user.dto;

import java.util.List;

public record UserProfileResponse(
    Long id,
    Long authUserId,
    String displayName,
    String profilePhoto,
    String phone,
    List<AddressResponse> addresses
) {}
