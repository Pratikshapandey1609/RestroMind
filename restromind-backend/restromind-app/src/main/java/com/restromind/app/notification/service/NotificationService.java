package com.restromind.app.notification.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.restromind.app.notification.dto.NotificationEvent;
import com.restromind.app.notification.dto.NotificationLogResponse;
import com.restromind.app.notification.dto.PreferenceRequest;
import com.restromind.app.notification.dto.PreferenceResponse;
import com.restromind.app.notification.entity.NotificationLog;
import com.restromind.app.notification.entity.NotificationPreference;
import com.restromind.app.notification.repository.NotificationLogRepository;
import com.restromind.app.notification.repository.NotificationPreferenceRepository;
import com.restromind.app.restaurant.dto.PageResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationLogRepository logRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    /**
     * Send a notification. Idempotent — if eventId already exists, silently skips.
     * Called internally by OrderService when order status changes.
     */
    @Transactional
    public void send(NotificationEvent event) {
        // Idempotency check — same eventId never processed twice
        if (logRepository.findByEventId(event.eventId()).isPresent()) {
            log.debug("Notification {} already processed, skipping", event.eventId());
            return;
        }

        // Check user preferences
        NotificationPreference prefs = getOrCreatePrefs(event.userId());
        boolean allowed = switch (event.channel().toUpperCase()) {
            case "PUSH"   -> prefs.isPushEnabled();
            case "EMAIL"  -> prefs.isEmailEnabled();
            case "IN_APP" -> prefs.isInAppEnabled();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unknown channel: " + event.channel());
        };

        NotificationLog entry = new NotificationLog();
        entry.setEventId(event.eventId());
        entry.setUserId(event.userId());
        entry.setChannel(event.channel().toUpperCase());
        entry.setTitle(event.title());
        entry.setBody(event.body());

        if (!allowed) {
            // User has disabled this channel — log as FAILED, don't deliver
            entry.setStatus("FAILED");
            logRepository.save(entry);
            log.info("Notification {} skipped — user {} disabled {} channel",
                event.eventId(), event.userId(), event.channel());
            return;
        }

        // Simulate delivery (in production: call FCM/email provider here)
        try {
            deliver(event);
            entry.setStatus("SENT");
            entry.setSentAt(Instant.now());
        } catch (Exception e) {
            entry.setStatus("FAILED");
            entry.setRetryCount(1);
            log.error("Failed to deliver notification {}: {}", event.eventId(), e.getMessage());
        }

        logRepository.save(entry);
    }

    // ── User-facing ───────────────────────────────────────────────────────────

    public PageResponse<NotificationLogResponse> getMyNotifications(Long userId, int page, int size) {
        Page<NotificationLog> result = logRepository
            .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, Math.min(size, 50)));
        List<NotificationLogResponse> content = result.getContent().stream()
            .map(this::toResponse).toList();
        return new PageResponse<>(content, result.getTotalElements(),
            result.getTotalPages(), page, size);
    }

    public PreferenceResponse getPreferences(Long userId) {
        NotificationPreference prefs = getOrCreatePrefs(userId);
        return new PreferenceResponse(prefs.isPushEnabled(), prefs.isEmailEnabled(), prefs.isInAppEnabled());
    }

    @Transactional
    public PreferenceResponse updatePreferences(Long userId, PreferenceRequest req) {
        NotificationPreference prefs = getOrCreatePrefs(userId);
        prefs.setPushEnabled(req.pushEnabled());
        prefs.setEmailEnabled(req.emailEnabled());
        prefs.setInAppEnabled(req.inAppEnabled());
        preferenceRepository.save(prefs);
        return new PreferenceResponse(prefs.isPushEnabled(), prefs.isEmailEnabled(), prefs.isInAppEnabled());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private NotificationPreference getOrCreatePrefs(Long userId) {
        return preferenceRepository.findByUserId(userId).orElseGet(() -> {
            NotificationPreference p = new NotificationPreference();
            p.setUserId(userId);
            return preferenceRepository.save(p);
        });
    }

    /**
     * Simulates channel delivery.
     * In production: integrate FCM (push), SendGrid/SES (email), WebSocket (in-app).
     */
    private void deliver(NotificationEvent event) {
        log.info("[{}] → user={} title='{}' body='{}'",
            event.channel().toUpperCase(), event.userId(), event.title(), event.body());
        // TODO: plug in real provider per channel
    }

    private NotificationLogResponse toResponse(NotificationLog n) {
        return new NotificationLogResponse(n.getId(), n.getEventId(), n.getChannel(),
            n.getTitle(), n.getBody(), n.getStatus(), n.getCreatedAt());
    }
}
