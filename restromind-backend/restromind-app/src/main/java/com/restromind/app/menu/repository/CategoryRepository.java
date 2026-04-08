package com.restromind.app.menu.repository;

import com.restromind.app.menu.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByRestaurantIdOrderBySortIndexAscIdAsc(Long restaurantId);
    Optional<Category> findByIdAndRestaurantId(Long id, Long restaurantId);
    boolean existsByRestaurantIdAndNameIgnoreCase(Long restaurantId, String name);
}
