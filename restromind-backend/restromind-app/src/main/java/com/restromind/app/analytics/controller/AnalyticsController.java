package com.restromind.app.analytics.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.restromind.app.analytics.dto.DashboardResponse;
import com.restromind.app.analytics.dto.RevenueReportResponse;
import com.restromind.app.analytics.dto.TopDishDto;
import com.restromind.app.analytics.service.AnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    private Long userId(HttpServletRequest req) {
        Object id = req.getAttribute("userId");
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        return (Long) id;
    }

    private void requireAdmin(HttpServletRequest req) {
        if (!"ADMIN".equals(req.getAttribute("role")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
    }

    @Operation(summary = "Dashboard — today's revenue, orders, last 7 days, top dishes")
    @GetMapping("/restaurants/{restaurantId}/dashboard")
    public ResponseEntity<DashboardResponse> dashboard(
            @PathVariable Long restaurantId,
            HttpServletRequest req) {
        requireAdmin(req);
        return ResponseEntity.ok(analyticsService.getDashboard(restaurantId, userId(req)));
    }

    @Operation(summary = "Revenue report for a date range")
    @GetMapping("/restaurants/{restaurantId}/revenue")
    public ResponseEntity<RevenueReportResponse> revenue(
            @PathVariable Long restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest req) {
        requireAdmin(req);
        return ResponseEntity.ok(analyticsService.getRevenueReport(restaurantId, userId(req), from, to));
    }

    @Operation(summary = "Top selling dishes for a date range")
    @GetMapping("/restaurants/{restaurantId}/top-dishes")
    public ResponseEntity<List<TopDishDto>> topDishes(
            @PathVariable Long restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest req) {
        requireAdmin(req);
        return ResponseEntity.ok(analyticsService.getTopDishes(restaurantId, userId(req), from, to, limit));
    }
}
