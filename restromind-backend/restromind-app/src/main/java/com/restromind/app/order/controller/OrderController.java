package com.restromind.app.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.restromind.app.order.dto.CreateOrderRequest;
import com.restromind.app.order.dto.OrderResponse;
import com.restromind.app.order.dto.UpdateStatusRequest;
import com.restromind.app.order.service.OrderService;
import com.restromind.app.restaurant.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    private Long userId(HttpServletRequest req) {
        Object id = req.getAttribute("userId");
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        return (Long) id;
    }

    private String role(HttpServletRequest req) {
        Object r = req.getAttribute("role");
        return r != null ? r.toString() : "";
    }

    // ── User: Place Order ─────────────────────────────────────────────────────

    @Tag(name = "Orders")
    @Operation(summary = "Place a new order")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest body,
            HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(orderService.createOrder(userId(req), body));
    }

    // ── User: My Orders ───────────────────────────────────────────────────────

    @Tag(name = "Orders")
    @Operation(summary = "Get my order history")
    @GetMapping("/me")
    public ResponseEntity<PageResponse<OrderResponse>> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest req) {
        return ResponseEntity.ok(orderService.getMyOrders(userId(req), page, size));
    }

    // ── Shared: Get Order Detail ──────────────────────────────────────────────

    @Tag(name = "Orders")
    @Operation(summary = "Get order detail by ID")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long id,
            HttpServletRequest req) {
        return ResponseEntity.ok(orderService.getOrder(id, userId(req), role(req)));
    }

    // ── Admin: Restaurant Orders ──────────────────────────────────────────────

    @Tag(name = "Orders")
    @Operation(summary = "Get all orders for a restaurant (admin only)")
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<PageResponse<OrderResponse>> restaurantOrders(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest req) {
        return ResponseEntity.ok(
            orderService.getOrdersForRestaurant(restaurantId, userId(req), page, size));
    }

    // ── Admin/User: Update Status ─────────────────────────────────────────────

    @Tag(name = "Orders")
    @Operation(summary = "Update order status (admin advances; user can cancel own order)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest body,
            HttpServletRequest req) {
        return ResponseEntity.ok(
            orderService.updateStatus(id, userId(req), role(req), body));
    }
}
