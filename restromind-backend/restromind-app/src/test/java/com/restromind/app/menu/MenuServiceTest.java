package com.restromind.app.menu;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.restromind.app.menu.dto.CategoryDto;
import com.restromind.app.menu.dto.CreateCategoryRequest;
import com.restromind.app.menu.dto.CreateDishRequest;
import com.restromind.app.menu.dto.DishDto;
import com.restromind.app.menu.dto.MenuDto;
import com.restromind.app.menu.entity.Category;
import com.restromind.app.menu.entity.Dish;
import com.restromind.app.menu.repository.CategoryRepository;
import com.restromind.app.menu.repository.DishRepository;
import com.restromind.app.menu.service.MenuService;
import com.restromind.app.restaurant.entity.Restaurant;
import com.restromind.app.restaurant.entity.RestaurantStatus;
import com.restromind.app.restaurant.repository.RestaurantRepository;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock CategoryRepository categoryRepository;
    @Mock DishRepository dishRepository;
    @Mock RestaurantRepository restaurantRepository;

    @InjectMocks MenuService menuService;

    private Restaurant restaurant;
    private Category category;
    private Dish dish;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setOwnerId(10L);
        restaurant.setName("Pizza Palace");
        restaurant.setStatus(RestaurantStatus.ACTIVE);

        category = new Category();
        category.setRestaurantId(1L);
        category.setName("Starters");
        category.setSortIndex(0);

        dish = new Dish();
        dish.setRestaurantId(1L);
        dish.setCategory(category);
        dish.setName("Garlic Bread");
        dish.setPrice(new BigDecimal("99.00"));
        dish.setAvailable(true);
    }

    // ── Get Menu ──────────────────────────────────────────────────────────────

    @Test
    void getMenu_restaurantNotFound_throws404() {
        when(restaurantRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> menuService.getMenu(1L, false))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getMenu_returnsOnlyAvailableDishes() {
        Dish unavailable = new Dish();
        unavailable.setName("Sold Out");
        unavailable.setPrice(BigDecimal.TEN);
        unavailable.setAvailable(false);
        category.getDishes().add(dish);
        category.getDishes().add(unavailable);

        when(restaurantRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.findByRestaurantIdOrderBySortIndexAscIdAsc(1L))
            .thenReturn(List.of(category));

        MenuDto menu = menuService.getMenu(1L, false);

        assertThat(menu.restaurantId()).isEqualTo(1L);
        assertThat(menu.categories()).hasSize(1);
        assertThat(menu.categories().get(0).dishes()).hasSize(1);
        assertThat(menu.categories().get(0).dishes().get(0).name()).isEqualTo("Garlic Bread");
    }

    @Test
    void getMenu_adminView_includesUnavailable() {
        Dish unavailable = new Dish();
        unavailable.setName("Sold Out");
        unavailable.setPrice(BigDecimal.TEN);
        unavailable.setAvailable(false);
        category.getDishes().add(dish);
        category.getDishes().add(unavailable);

        when(restaurantRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.findByRestaurantIdOrderBySortIndexAscIdAsc(1L))
            .thenReturn(List.of(category));

        MenuDto menu = menuService.getMenu(1L, true);

        assertThat(menu.categories().get(0).dishes()).hasSize(2);
    }

    // ── Create Category ───────────────────────────────────────────────────────

    @Test
    void createCategory_ownerMismatch_throws403() {
        when(restaurantRepository.findByOwnerId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.createCategory(1L, 99L,
                new CreateCategoryRequest("Mains", 1)))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createCategory_duplicate_throws409() {
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.existsByRestaurantIdAndNameIgnoreCase(1L, "Starters"))
            .thenReturn(true);

        assertThatThrownBy(() -> menuService.createCategory(1L, 10L,
                new CreateCategoryRequest("Starters", 0)))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createCategory_valid_returnsCategoryDto() {
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.existsByRestaurantIdAndNameIgnoreCase(1L, "Mains"))
            .thenReturn(false);
        when(categoryRepository.save(any())).thenReturn(category);

        CategoryDto dto = menuService.createCategory(1L, 10L,
            new CreateCategoryRequest("Mains", 1));

        assertThat(dto.name()).isEqualTo("Starters"); // returned from mock
        verify(categoryRepository).save(any(Category.class));
    }

    // ── Create Dish ───────────────────────────────────────────────────────────

    @Test
    void createDish_categoryNotFound_throws404() {
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findByIdAndRestaurantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.createDish(1L, 99L, 10L,
                new CreateDishRequest("Pizza", null, BigDecimal.TEN, null, null)))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createDish_valid_returnsDishDto() {
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findByIdAndRestaurantId(1L, 1L)).thenReturn(Optional.of(category));
        when(dishRepository.save(any())).thenReturn(dish);

        DishDto dto = menuService.createDish(1L, 1L, 10L,
            new CreateDishRequest("Garlic Bread", "Crispy", new BigDecimal("99.00"), null, null));

        assertThat(dto.name()).isEqualTo("Garlic Bread");
        assertThat(dto.price()).isEqualByComparingTo("99.00");
    }

    // ── Toggle Availability ───────────────────────────────────────────────────

    @Test
    void toggleAvailability_setsCorrectly() {
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(restaurant));
        when(dishRepository.findActiveByIdAndRestaurantId(1L, 1L)).thenReturn(Optional.of(dish));
        when(dishRepository.save(any())).thenReturn(dish);

        menuService.toggleAvailability(1L, 1L, 10L, false);

        assertThat(dish.isAvailable()).isFalse();
    }

    // ── Soft Delete Dish ──────────────────────────────────────────────────────

    @Test
    void deleteDish_setsDeletedAt() {
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(restaurant));
        when(dishRepository.findActiveByIdAndRestaurantId(1L, 1L)).thenReturn(Optional.of(dish));
        when(dishRepository.save(any())).thenReturn(dish);

        menuService.deleteDish(1L, 1L, 10L);

        assertThat(dish.getDeletedAt()).isNotNull();
    }

    // ── isDishAvailable ───────────────────────────────────────────────────────

    @Test
    void isDishAvailable_available_returnsTrue() {
        when(dishRepository.findActiveById(1L)).thenReturn(Optional.of(dish));
        assertThat(menuService.isDishAvailable(1L)).isTrue();
    }

    @Test
    void isDishAvailable_notFound_returnsFalse() {
        when(dishRepository.findActiveById(99L)).thenReturn(Optional.empty());
        assertThat(menuService.isDishAvailable(99L)).isFalse();
    }
}
