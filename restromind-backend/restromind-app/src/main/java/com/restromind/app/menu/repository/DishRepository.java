package com.restromind.app.menu.repository;

import com.restromind.app.menu.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DishRepository extends JpaRepository<Dish, Long> {

    // Only non-deleted dishes
    @Query("SELECT d FROM Dish d WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<Dish> findActiveById(@Param("id") Long id);

    // Validate dish belongs to restaurant and is not deleted
    @Query("SELECT d FROM Dish d WHERE d.id = :id AND d.restaurantId = :restaurantId AND d.deletedAt IS NULL")
    Optional<Dish> findActiveByIdAndRestaurantId(@Param("id") Long id, @Param("restaurantId") Long restaurantId);
}
