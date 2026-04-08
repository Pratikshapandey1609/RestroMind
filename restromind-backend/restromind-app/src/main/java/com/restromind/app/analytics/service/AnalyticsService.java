package com.restromind.app.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.restromind.app.analytics.dto.DailyRevenueDto;
import com.restromind.app.analytics.dto.DashboardResponse;
import com.restromind.app.analytics.dto.RevenueReportResponse;
import com.restromind.app.analytics.dto.TopDishDto;
import com.restromind.app.analytics.entity.AnalyticsSummary;
import com.restromind.app.analytics.entity.TopDishCache;
import com.restromind.app.analytics.repository.AnalyticsSummaryRepository;
import com.restromind.app.analytics.repository.TopDishCacheRepository;
import com.restromind.app.order.entity.Order;
import com.restromind.app.order.repository.OrderRepository;
import com.restromind.app.restaurant.repository.RestaurantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsSummaryRepository summaryRepository;
    private final TopDishCacheRepository topDishRepository;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    public DashboardResponse getDashboard(Long restaurantId, Long ownerId) {
        requireOwnership(restaurantId, ownerId);

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);

        BigDecimal todayRevenue = summaryRepository.getTodayRevenue(restaurantId, today);
        int todayOrders = summaryRepository.getTodayOrders(restaurantId, today);
        BigDecimal avg = todayOrders > 0
            ? todayRevenue.divide(BigDecimal.valueOf(todayOrders), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        List<DailyRevenueDto> last7 = summaryRepository
            .findByRestaurantIdAndSummaryDateBetweenOrderBySummaryDateAsc(restaurantId, weekAgo, today)
            .stream()
            .map(s -> new DailyRevenueDto(s.getSummaryDate(), s.getTotalRevenue(), s.getTotalOrders()))
            .toList();

        List<TopDishDto> topDishes = topDishRepository
            .findTopDishes(restaurantId, weekAgo, today, PageRequest.of(0, 5))
            .stream()
            .map(t -> new TopDishDto(t.getDishId(), t.getDishName(), t.getQuantitySold(), t.getRevenue()))
            .toList();

        return new DashboardResponse(todayRevenue, todayOrders, avg, last7, topDishes);
    }

    // ── Revenue Report ────────────────────────────────────────────────────────

    public RevenueReportResponse getRevenueReport(Long restaurantId, Long ownerId,
                                                   LocalDate from, LocalDate to) {
        requireOwnership(restaurantId, ownerId);
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to");
        }

        List<AnalyticsSummary> summaries = summaryRepository
            .findByRestaurantIdAndSummaryDateBetweenOrderBySummaryDateAsc(restaurantId, from, to);

        BigDecimal total = summaries.stream()
            .map(AnalyticsSummary::getTotalRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        int orders = summaries.stream().mapToInt(AnalyticsSummary::getTotalOrders).sum();
        BigDecimal avg = orders > 0
            ? total.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        List<DailyRevenueDto> daily = summaries.stream()
            .map(s -> new DailyRevenueDto(s.getSummaryDate(), s.getTotalRevenue(), s.getTotalOrders()))
            .toList();

        return new RevenueReportResponse(total, orders, avg, daily);
    }

    // ── Top Dishes ────────────────────────────────────────────────────────────

    public List<TopDishDto> getTopDishes(Long restaurantId, Long ownerId,
                                          LocalDate from, LocalDate to, int limit) {
        requireOwnership(restaurantId, ownerId);
        return topDishRepository
            .findTopDishes(restaurantId, from, to, PageRequest.of(0, Math.min(limit, 50)))
            .stream()
            .map(t -> new TopDishDto(t.getDishId(), t.getDishName(), t.getQuantitySold(), t.getRevenue()))
            .toList();
    }

    // ── Internal: Update summaries when order is delivered ───────────────────

    @Transactional
    public void recordDeliveredOrder(Order order) {
        LocalDate date = order.getDeliveredAt() != null
            ? order.getDeliveredAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            : LocalDate.now();

        AnalyticsSummary summary = summaryRepository
            .findByRestaurantIdAndSummaryDate(order.getRestaurantId(), date)
            .orElseGet(() -> {
                AnalyticsSummary s = new AnalyticsSummary();
                s.setRestaurantId(order.getRestaurantId());
                s.setSummaryDate(date);
                return s;
            });

        summary.setTotalRevenue(summary.getTotalRevenue().add(order.getGrandTotal()));
        summary.setTotalOrders(summary.getTotalOrders() + 1);
        int newCount = summary.getTotalOrders();
        summary.setAvgOrderValue(
            summary.getTotalRevenue().divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP));
        summaryRepository.save(summary);

        // Update top dishes cache
        order.getItems().forEach(item -> {
            TopDishCache cache = new TopDishCache();
            cache.setRestaurantId(order.getRestaurantId());
            cache.setDishId(item.getDishId());
            cache.setDishName(item.getDishName());
            cache.setQuantitySold(item.getQuantity());
            cache.setRevenue(item.getLineTotal());
            cache.setPeriodStart(date);
            cache.setPeriodEnd(date);
            topDishRepository.save(cache);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void requireOwnership(Long restaurantId, Long ownerId) {
        restaurantRepository.findByOwnerId(ownerId)
            .filter(r -> r.getId().equals(restaurantId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You do not own this restaurant"));
    }
}
