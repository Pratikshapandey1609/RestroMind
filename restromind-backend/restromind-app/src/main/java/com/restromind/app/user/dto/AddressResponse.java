package com.restromind.app.user.dto;

public record AddressResponse(Long id, String label, String address, String city, boolean isDefault) {}
