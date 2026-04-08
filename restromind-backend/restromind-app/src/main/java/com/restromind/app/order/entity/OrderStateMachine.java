package com.restromind.app.order.entity;

import java.util.Map;
import java.util.Set;

public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
        OrderStatus.PENDING,           Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
        OrderStatus.CONFIRMED,         Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
        OrderStatus.PREPARING,         Set.of(OrderStatus.READY, OrderStatus.CANCELLED),
        OrderStatus.READY,             Set.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED),
        OrderStatus.OUT_FOR_DELIVERY,  Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
        OrderStatus.DELIVERED,         Set.of(),
        OrderStatus.CANCELLED,         Set.of()
    );

    public static boolean isValid(OrderStatus from, OrderStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
