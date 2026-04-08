package com.restromind.app.order.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.restromind.app.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId, Pageable pageable);
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
