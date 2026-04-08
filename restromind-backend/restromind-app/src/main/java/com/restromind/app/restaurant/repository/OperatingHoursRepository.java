package com.restromind.app.restaurant.repository;

import com.restromind.app.restaurant.entity.OperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OperatingHoursRepository extends JpaRepository<OperatingHours, Long> {
    List<OperatingHours> findByRestaurantIdOrderByDayOfWeek(Long restaurantId);

    @Modifying @Transactional
    void deleteByRestaurantId(Long restaurantId);
}
