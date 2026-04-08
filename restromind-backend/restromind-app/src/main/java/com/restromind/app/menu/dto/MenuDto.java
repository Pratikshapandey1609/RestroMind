package com.restromind.app.menu.dto;

import java.util.List;

public record MenuDto(Long restaurantId, List<CategoryDto> categories) {}
