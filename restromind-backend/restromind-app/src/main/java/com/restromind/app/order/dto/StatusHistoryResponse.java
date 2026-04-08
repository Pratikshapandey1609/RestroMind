package com.restromind.app.order.dto;

import java.time.Instant;

public record StatusHistoryResponse(String status, Instant changedAt) {}
