package com.restromind.app.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record RevenueReportResponse(
    BigDecimal totalRevenue,
    int totalOrders,
    BigDecimal avgOrderValue,
    List<DailyRevenueDto> dailyBreakdown
) {}
