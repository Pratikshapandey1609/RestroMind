package com.restromind.app.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
    @Size(max = 255) String fullName,
    @Email String email
) {}
