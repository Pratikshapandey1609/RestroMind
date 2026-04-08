package com.restromind.app.menu.service;

import com.restromind.app.menu.dto.*;
import com.restromind.app.menu.entity.Category;
import com.restromind.app.menu.entity.Dish;
import com.restromind.app.menu.repository.CategoryRepository;
import com.restromind.app.menu.repository.DishRepository;
import com.restromind.app.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final CategoryRepository categoryRepository;
    private final DishRepository dishRepository;
    private final RestaurantRepository restaurantRepository;

    // ── Public / User-facing ──────────────────────────────────────────────────

    public MenuDto getMenu(Long restaurantId, boolean includeUnavailable) {
        requireRestaurantExists(restaurantId);
        List<Category> categories = categoryRepository
            .findByRestaurantIdOrderBySortIndexAscIdAsc(restaurantId);

        List<CategoryDto> categoryDtos = categories.stream()
            .map(c -> {
                List<DishDto> dishes = c.getDishes().stream()
                    .filter(d -> d.getDeletedAt() == null)
                    .filter(d -> includeUnavailable || d.isAvailable())
                    .map(this::toDishDto)
                    .toList();
                return new CategoryDto(c.getId(), c.getName(), c.getSortIndex(), dishes);
            })
            .toList();

        return new MenuDto(restaurantId, categoryDtos);
    }

    public DishDto getDish(Long dishId) {
        Dish dish = dishRepository.findActiveById(dishId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));
        return toDishDto(dish);
    }

    // ── Admin — Categories ────────────────────────────────────────────────────

    @Transactional
    public CategoryDto createCategory(Long restaurantId, Long ownerId, CreateCategoryRequest req) {
        requireOwnership(restaurantId, ownerId);
        if (categoryRepository.existsByRestaurantIdAndNameIgnoreCase(restaurantId, req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Category '" + req.name() + "' already exists for this restaurant");
        }
        Category cat = new Category();
        cat.setRestaurantId(restaurantId);
        cat.setName(req.name());
        cat.setSortIndex(req.sortIndex());
        Category saved = categoryRepository.save(cat);
        return new CategoryDto(saved.getId(), saved.getName(), saved.getSortIndex(), List.of());
    }

    @Transactional
    public CategoryDto updateCategory(Long restaurantId, Long categoryId, Long ownerId,
                                      CreateCategoryRequest req) {
        requireOwnership(restaurantId, ownerId);
        Category cat = categoryRepository.findByIdAndRestaurantId(categoryId, restaurantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        cat.setName(req.name());
        cat.setSortIndex(req.sortIndex());
        Category saved = categoryRepository.save(cat);
        return new CategoryDto(saved.getId(), saved.getName(), saved.getSortIndex(),
            saved.getDishes().stream().filter(d -> d.getDeletedAt() == null)
                .map(this::toDishDto).toList());
    }

    @Transactional
    public void deleteCategory(Long restaurantId, Long categoryId, Long ownerId) {
        requireOwnership(restaurantId, ownerId);
        Category cat = categoryRepository.findByIdAndRestaurantId(categoryId, restaurantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        // Soft-delete all dishes in this category
        cat.getDishes().forEach(d -> d.setDeletedAt(Instant.now()));
        categoryRepository.delete(cat);
    }

    // ── Admin — Dishes ────────────────────────────────────────────────────────

    @Transactional
    public DishDto createDish(Long restaurantId, Long categoryId, Long ownerId,
                              CreateDishRequest req) {
        requireOwnership(restaurantId, ownerId);
        Category cat = categoryRepository.findByIdAndRestaurantId(categoryId, restaurantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        Dish dish = new Dish();
        dish.setCategory(cat);
        dish.setRestaurantId(restaurantId);
        dish.setName(req.name());
        dish.setDescription(req.description());
        dish.setPrice(req.price());
        dish.setImageUrl(req.imageUrl());
        dish.setAllergens(req.allergens());
        dish.setAvailable(true);
        return toDishDto(dishRepository.save(dish));
    }

    @Transactional
    public DishDto updateDish(Long restaurantId, Long dishId, Long ownerId,
                              UpdateDishRequest req) {
        requireOwnership(restaurantId, ownerId);
        Dish dish = dishRepository.findActiveByIdAndRestaurantId(dishId, restaurantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));
        dish.setName(req.name());
        dish.setDescription(req.description());
        dish.setPrice(req.price());
        dish.setImageUrl(req.imageUrl());
        dish.setAllergens(req.allergens());
        return toDishDto(dishRepository.save(dish));
    }

    @Transactional
    public void deleteDish(Long restaurantId, Long dishId, Long ownerId) {
        requireOwnership(restaurantId, ownerId);
        Dish dish = dishRepository.findActiveByIdAndRestaurantId(dishId, restaurantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));
        dish.setDeletedAt(Instant.now());
        dishRepository.save(dish);
    }

    @Transactional
    public DishDto toggleAvailability(Long restaurantId, Long dishId, Long ownerId,
                                      boolean available) {
        requireOwnership(restaurantId, ownerId);
        Dish dish = dishRepository.findActiveByIdAndRestaurantId(dishId, restaurantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));
        dish.setAvailable(available);
        return toDishDto(dishRepository.save(dish));
    }

    // ── Internal (used by Order Service) ─────────────────────────────────────

    public boolean isDishAvailable(Long dishId) {
        return dishRepository.findActiveById(dishId)
            .map(Dish::isAvailable)
            .orElse(false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void requireRestaurantExists(Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }
    }

    private void requireOwnership(Long restaurantId, Long ownerId) {
        restaurantRepository.findByOwnerId(ownerId)
            .filter(r -> r.getId().equals(restaurantId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You do not own this restaurant"));
    }

    private DishDto toDishDto(Dish d) {
        return new DishDto(d.getId(), d.getName(), d.getDescription(),
            d.getPrice(), d.getImageUrl(), d.getAllergens(), d.isAvailable());
    }
}
