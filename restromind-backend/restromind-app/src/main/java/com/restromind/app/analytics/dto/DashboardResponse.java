package com.restromind.app.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
    BigDecimal todayRevenue,
    int todayOrders,
    BigDecimal avgOrderValue,
    List<DailyRevenueDto> last7Days,
    List<TopDishDto> topDishes
) {}
