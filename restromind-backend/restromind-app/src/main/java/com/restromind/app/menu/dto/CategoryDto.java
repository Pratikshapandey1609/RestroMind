package com.restromind.app.menu.dto;

import java.util.List;

public record CategoryDto(Long id, String name, int sortIndex, List<DishDto> dishes) {}
