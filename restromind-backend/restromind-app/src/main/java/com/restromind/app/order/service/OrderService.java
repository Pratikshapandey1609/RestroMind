package com.restromind.app.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.restromind.app.analytics.service.AnalyticsService;
import com.restromind.app.menu.service.MenuService;
import com.restromind.app.notification.dto.NotificationEvent;
import com.restromind.app.notification.service.NotificationService;
import com.restromind.app.order.dto.CreateOrderRequest;
import com.restromind.app.order.dto.OrderItemRequest;
import com.restromind.app.order.dto.OrderItemResponse;
import com.restromind.app.order.dto.OrderPrintResponse;
import com.restromind.app.order.dto.OrderResponse;
import com.restromind.app.order.dto.StatusHistoryResponse;
import com.restromind.app.order.dto.UpdateStatusRequest;
import com.restromind.app.order.entity.Order;
import com.restromind.app.order.entity.OrderItem;
import com.restromind.app.order.entity.OrderStateMachine;
import com.restromind.app.order.entity.OrderStatus;
import com.restromind.app.order.entity.OrderStatusHistory;
import com.restromind.app.order.repository.OrderRepository;
import com.restromind.app.restaurant.dto.PageResponse;
import com.restromind.app.restaurant.repository.RestaurantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuService menuService;
    private final AnalyticsService analyticsService;
    private final NotificationService notificationService;

    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest req) {
        // Validate restaurant exists and is open
        var restaurant = restaurantRepository.findById(req.restaurantId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

        var status = restaurant.getStatus();
        if (status != com.restromind.app.restaurant.entity.RestaurantStatus.OPEN
                && status != com.restromind.app.restaurant.entity.RestaurantStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Restaurant is not accepting orders");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setRestaurantId(req.restaurantId());
        order.setDeliveryAddress(req.deliveryAddress());
        order.setSpecialInstructions(req.specialInstructions());

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : req.items()) {
            // Validate dish is available
            if (!menuService.isDishAvailable(itemReq.dishId())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Dish " + itemReq.dishId() + " is not available");
            }
            var dish = menuService.getDish(itemReq.dishId());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setDishId(dish.id());
            item.setDishName(dish.name());
            item.setUnitPrice(dish.price());
            item.setQuantity(itemReq.quantity());
            BigDecimal lineTotal = dish.price().multiply(BigDecimal.valueOf(itemReq.quantity()));
            item.setLineTotal(lineTotal);
            order.getItems().add(item);
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal deliveryFee = BigDecimal.ZERO;
        order.setSubtotal(subtotal);
        order.setDeliveryFee(deliveryFee);
        order.setGrandTotal(subtotal.add(deliveryFee));

        // Record initial status history
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.PENDING);
        history.setChangedBy(userId);
        order.getStatusHistory().add(history);

        Order saved = orderRepository.save(order);

        // Notify user — order placed
        notificationService.send(new NotificationEvent(
            java.util.UUID.randomUUID(), userId,
            "IN_APP", "Order Placed",
            "Your order #" + saved.getId() + " has been placed successfully."));

        return toResponse(saved);
    }

    public OrderResponse getOrder(Long orderId, Long requesterId, String role) {
        Order order = findOrder(orderId);
        // Users can only see their own orders; admins can see all
        if (!"ADMIN".equals(role) && !order.getUserId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return toResponse(order);
    }

    public PageResponse<OrderResponse> getOrdersForRestaurant(Long restaurantId, Long ownerId, int page, int size) {
        return getOrdersForRestaurantFiltered(restaurantId, ownerId, null, page, size);
    }

    public PageResponse<OrderResponse> getOrdersForRestaurantFiltered(Long restaurantId, Long ownerId,
                                                                       String statusFilter, int page, int size) {
        restaurantRepository.findByOwnerId(ownerId)
            .filter(r -> r.getId().equals(restaurantId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this restaurant"));

        Page<Order> result;
        if (statusFilter != null && !statusFilter.isBlank()) {
            OrderStatus status;
            try { status = OrderStatus.valueOf(statusFilter.toUpperCase()); }
            catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + statusFilter);
            }
            result = orderRepository.findByRestaurantIdAndStatusOrderByCreatedAtDesc(
                restaurantId, status, PageRequest.of(page, Math.min(size, 100)));
        } else {
            result = orderRepository.findByRestaurantIdOrderByCreatedAtDesc(
                restaurantId, PageRequest.of(page, Math.min(size, 100)));
        }
        return toPageResponse(result, page, size);
    }

    public OrderPrintResponse getPrintView(Long orderId, Long requesterId, String role) {
        Order order = findOrder(orderId);
        if (!"ADMIN".equals(role) && !order.getUserId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        var restaurant = restaurantRepository.findById(order.getRestaurantId()).orElse(null);
        String restaurantName = restaurant != null ? restaurant.getName() : "Unknown";

        List<OrderItemResponse> items = order.getItems().stream()
            .map(i -> new OrderItemResponse(i.getDishId(), i.getDishName(),
                i.getUnitPrice(), i.getQuantity(), i.getLineTotal()))
            .toList();

        return new OrderPrintResponse(order.getId(), restaurantName, order.getStatus().name(),
            order.getCreatedAt(), order.getDeliveryAddress(), order.getSpecialInstructions(),
            items, order.getSubtotal(), order.getDeliveryFee(), order.getGrandTotal());
    }

    public PageResponse<OrderResponse> getMyOrders(Long userId, int page, int size) {
        Page<Order> result = orderRepository.findByUserIdOrderByCreatedAtDesc(
            userId, PageRequest.of(page, Math.min(size, 100)));
        return toPageResponse(result, page, size);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, Long requesterId, String role, UpdateStatusRequest req) {
        Order order = findOrder(orderId);

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(req.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + req.status());
        }

        if (!OrderStateMachine.isValid(order.getStatus(), newStatus)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Cannot transition from " + order.getStatus() + " to " + newStatus);
        }

        // Only admin (restaurant owner) can advance status; user can only cancel their own order
        if (newStatus == OrderStatus.CANCELLED) {
            if (!"ADMIN".equals(role) && !order.getUserId().equals(requesterId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
            order.setCancellationReason(req.cancellationReason());
            order.setCancelledAt(Instant.now());
        } else {
            if (!"ADMIN".equals(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only restaurant admins can update order status");
            }
        }

        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(Instant.now());
            // Record analytics after save
        }

        order.setStatus(newStatus);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(newStatus);
        history.setChangedBy(requesterId);
        order.getStatusHistory().add(history);

        Order saved = orderRepository.save(order);

        // Notify user of status change
        String title = "Order #" + saved.getId() + " " + newStatus.name().replace("_", " ");
        String body = switch (newStatus) {
            case CONFIRMED        -> "Your order has been confirmed by the restaurant.";
            case PREPARING        -> "The restaurant is preparing your order.";
            case READY            -> "Your order is ready for pickup/delivery.";
            case OUT_FOR_DELIVERY -> "Your order is on the way!";
            case DELIVERED        -> "Your order has been delivered. Enjoy!";
            case CANCELLED        -> "Your order has been cancelled. " +
                (req.cancellationReason() != null ? req.cancellationReason() : "");
            default -> "";
        };
        notificationService.send(new NotificationEvent(
            java.util.UUID.randomUUID(), saved.getUserId(), "IN_APP", title, body));

        // Record analytics when delivered
        if (newStatus == OrderStatus.DELIVERED) {
            analyticsService.recordDeliveredOrder(saved);
        }

        return toResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private OrderResponse toResponse(Order o) {
        List<OrderItemResponse> items = o.getItems().stream()
            .map(i -> new OrderItemResponse(i.getDishId(), i.getDishName(),
                i.getUnitPrice(), i.getQuantity(), i.getLineTotal()))
            .toList();
        List<StatusHistoryResponse> history = o.getStatusHistory().stream()
            .map(h -> new StatusHistoryResponse(h.getStatus().name(), h.getChangedAt()))
            .toList();
        return new OrderResponse(o.getId(), o.getUserId(), o.getRestaurantId(),
            o.getStatus().name(), o.getSubtotal(), o.getDeliveryFee(), o.getGrandTotal(),
            o.getDeliveryAddress(), o.getSpecialInstructions(), o.getCancellationReason(),
            o.getCreatedAt(), items, history);
    }

    private PageResponse<OrderResponse> toPageResponse(Page<Order> page, int pageNum, int size) {
        List<OrderResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(content, page.getTotalElements(),
            page.getTotalPages(), pageNum, size);
    }
}
