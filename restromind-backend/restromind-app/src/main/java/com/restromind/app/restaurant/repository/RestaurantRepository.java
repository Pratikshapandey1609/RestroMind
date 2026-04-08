package com.restromind.app.restaurant.repository;

import com.restromind.app.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByOwnerId(Long ownerId);

    @Query("""
        SELECT DISTINCT r FROM Restaurant r
        LEFT JOIN r.operatingHours oh
        WHERE r.status = com.restromind.app.restaurant.entity.RestaurantStatus.OPEN
           OR (r.status = com.restromind.app.restaurant.entity.RestaurantStatus.ACTIVE
               AND oh.dayOfWeek = :dayOfWeek
               AND oh.openTime <= :currentTime
               AND oh.closeTime > :currentTime)
        """)
    List<Restaurant> findAvailableRestaurants(
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        @Param("currentTime") LocalTime currentTime);

    @Query("""
        SELECT r FROM Restaurant r
        WHERE r.status IN (
            com.restromind.app.restaurant.entity.RestaurantStatus.ACTIVE,
            com.restromind.app.restaurant.entity.RestaurantStatus.OPEN)
        AND LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%'))
        """)
    List<Restaurant> searchByName(@Param("query") String query);
}
