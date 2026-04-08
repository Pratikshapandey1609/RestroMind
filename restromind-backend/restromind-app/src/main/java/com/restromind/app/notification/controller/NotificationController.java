package com.restromind.app.notification.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.restromind.app.notification.dto.NotificationEvent;
import com.restromind.app.notification.dto.NotificationLogResponse;
import com.restromind.app.notification.dto.PreferenceRequest;
import com.restromind.app.notification.dto.PreferenceResponse;
import com.restromind.app.notification.service.NotificationService;
import com.restromind.app.restaurant.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    private Long userId(HttpServletRequest req) {
        Object id = req.getAttribute("userId");
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        return (Long) id;
    }

    // ── User: notification history ────────────────────────────────────────────

    @Operation(summary = "Get my notification history")
    @GetMapping("/me")
    public ResponseEntity<PageResponse<NotificationLogResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest req) {
        return ResponseEntity.ok(notificationService.getMyNotifications(userId(req), page, size));
    }

    // ── User: preferences ─────────────────────────────────────────────────────

    @Operation(summary = "Get my notification preferences")
    @GetMapping("/preferences")
    public ResponseEntity<PreferenceResponse> getPreferences(HttpServletRequest req) {
        return ResponseEntity.ok(notificationService.getPreferences(userId(req)));
    }

    @Operation(summary = "Update my notification preferences")
    @PutMapping("/preferences")
    public ResponseEntity<PreferenceResponse> updatePreferences(
            @RequestBody PreferenceRequest body,
            HttpServletRequest req) {
        return ResponseEntity.ok(notificationService.updatePreferences(userId(req), body));
    }

    // ── Internal: send notification (called by other services) ───────────────

    @Operation(summary = "Send a notification (internal use — called by Order/Restaurant services)")
    @PostMapping("/send")
    public ResponseEntity<Void> send(@Valid @RequestBody NotificationEvent event) {
        notificationService.send(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
