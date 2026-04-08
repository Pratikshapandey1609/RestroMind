package com.restromind.app.analytics.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.restromind.app.analytics.entity.AnalyticsSummary;

public interface AnalyticsSummaryRepository extends JpaRepository<AnalyticsSummary, Long> {

    Optional<AnalyticsSummary> findByRestaurantIdAndSummaryDate(Long restaurantId, LocalDate date);

    List<AnalyticsSummary> findByRestaurantIdAndSummaryDateBetweenOrderBySummaryDateAsc(
        Long restaurantId, LocalDate from, LocalDate to);

    @Query("""
        SELECT COALESCE(SUM(a.totalRevenue), 0) FROM AnalyticsSummary a
        WHERE a.restaurantId = :restaurantId AND a.summaryDate = :date
        """)
    java.math.BigDecimal getTodayRevenue(@Param("restaurantId") Long restaurantId,
                                          @Param("date") LocalDate date);

    @Query("""
        SELECT COALESCE(SUM(a.totalOrders), 0) FROM AnalyticsSummary a
        WHERE a.restaurantId = :restaurantId AND a.summaryDate = :date
        """)
    int getTodayOrders(@Param("restaurantId") Long restaurantId, @Param("date") LocalDate date);
}
