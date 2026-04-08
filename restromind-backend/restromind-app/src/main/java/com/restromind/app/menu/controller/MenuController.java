package com.restromind.app.menu.controller;

import com.restromind.app.menu.dto.*;
import com.restromind.app.menu.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    private Long userId(HttpServletRequest req) {
        Object id = req.getAttribute("userId");
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        return (Long) id;
    }

    private void requireAdmin(HttpServletRequest req) {
        if (!"ADMIN".equals(req.getAttribute("role")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
    }

    // ── Public ────────────────────────────────────────────────────────────────

    @Tag(name = "Menu")
    @Operation(summary = "Get full menu for a restaurant (available dishes only)")
    @GetMapping("/restaurants/{restaurantId}")
    public ResponseEntity<MenuDto> getMenu(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(menuService.getMenu(restaurantId, false));
    }

    @Tag(name = "Menu")
    @Operation(summary = "Get single dish by ID")
    @GetMapping("/dishes/{dishId}")
    public ResponseEntity<DishDto> getDish(@PathVariable Long dishId) {
        return ResponseEntity.ok(menuService.getDish(dishId));
    }

    // ── Admin — Categories ────────────────────────────────────────────────────

    @Tag(name = "Menu Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get full menu including unavailable dishes (admin view)")
    @GetMapping("/restaurants/{restaurantId}/admin")
    public ResponseEntity<MenuDto> getMenuAdmin(
            @PathVariable Long restaurantId, HttpServletRequest req) {
        requireAdmin(req);
        return ResponseEntity.ok(menuService.getMenu(restaurantId, true));
    }

    @Tag(name = "Menu Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new category")
    @PostMapping("/restaurants/{restaurantId}/categories")
    public ResponseEntity<CategoryDto> createCategory(
            @PathVariable Long restaurantId,
            @Valid @RequestBody CreateCategoryRequest body,
            HttpServletRequest req) {
        requireAdmin(req);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(menuService.createCategory(restaurantId, userId(req), body));
    }

    @Tag(name = "Menu Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a category")
    @PutMapping("/restaurants/{restaurantId}/categories/{categoryId}")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable Long restaurantId,
            @PathVariable Long categoryId,
            @Valid @RequestBody CreateCategoryRequest body,
            HttpServletRequest req) {
        requireAdmin(req);
        return ResponseEntity.ok(menuService.updateCategory(restaurantId, categoryId, userId(req), body));
    }

    @Tag(name = "Menu Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a category and all its dishes")
    @DeleteMapping("/restaurants/{restaurantId}/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long restaurantId,
            @PathVariable Long categoryId,
            HttpServletRequest req) {
        requireAdmin(req);
        menuService.deleteCategory(restaurantId, categoryId, userId(req));
        return ResponseEntity.noContent().build();
    }

    // ── Admin — Dishes ────────────────────────────────────────────────────────

    @Tag(name = "Menu Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add a dish to a category")
    @PostMapping("/restaurants/{restaurantId}/categories/{categoryId}/dishes")
    public ResponseEntity<DishDto> createDish(
            @PathVariable Long restaurantId,
            @PathVariable Long categoryId,
            @Valid @RequestBody CreateDishRequest body,
            HttpServletRequest req) {
        requireAdmin(req);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(menuService.createDish(restaurantId, categoryId, userId(req), body));
    }

    @Tag(name = "Menu Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a dish")
    @PutMapping("/restaurants/{restaurantId}/dishes/{dishId}")
    public ResponseEntity<DishDto> updateDish(
            @PathVariable Long restaurantId,
            @PathVariable Long dishId,
            @Valid @RequestBody UpdateDishRequest body,
            HttpServletRequest req) {
        requireAdmin(req);
        return ResponseEntity.ok(menuService.updateDish(restaurantId, dishId, userId(req), body));
    }

    @Tag(name = "Menu Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Soft-delete a dish")
    @DeleteMapping("/restaurants/{restaurantId}/dishes/{dishId}")
    public ResponseEntity<Void> deleteDish(
            @PathVariable Long restaurantId,
            @PathVariable Long dishId,
            HttpServletRequest req) {
        requireAdmin(req);
        menuService.deleteDish(restaurantId, dishId, userId(req));
        return ResponseEntity.noContent().build();
    }

    @Tag(name = "Menu Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Toggle dish availability")
    @PatchMapping("/restaurants/{restaurantId}/dishes/{dishId}/availability")
    public ResponseEntity<DishDto> toggleAvailability(
            @PathVariable Long restaurantId,
            @PathVariable Long dishId,
            @RequestBody AvailabilityRequest body,
            HttpServletRequest req) {
        requireAdmin(req);
        return ResponseEntity.ok(menuService.toggleAvailability(restaurantId, dishId, userId(req), body.available()));
    }
}
