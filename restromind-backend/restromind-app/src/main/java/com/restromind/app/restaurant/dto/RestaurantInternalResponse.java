package com.restromind.app.restaurant.dto;

public record RestaurantInternalResponse(Long id, String name, Long ownerId, String status, String cuisineType) {}
