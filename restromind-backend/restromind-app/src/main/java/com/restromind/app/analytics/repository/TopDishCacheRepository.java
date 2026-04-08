package com.restromind.app.analytics.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.restromind.app.analytics.entity.TopDishCache;

public interface TopDishCacheRepository extends JpaRepository<TopDishCache, Long> {

    @Query("""
        SELECT t FROM TopDishCache t
        WHERE t.restaurantId = :restaurantId
          AND t.periodStart >= :from AND t.periodEnd <= :to
        ORDER BY t.quantitySold DESC
        """)
    List<TopDishCache> findTopDishes(@Param("restaurantId") Long restaurantId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to,
                                     Pageable pageable);
}
