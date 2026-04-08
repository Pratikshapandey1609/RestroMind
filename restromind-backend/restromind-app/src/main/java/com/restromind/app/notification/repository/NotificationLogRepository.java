package com.restromind.app.notification.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.restromind.app.notification.entity.NotificationLog;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    Optional<NotificationLog> findByEventId(UUID eventId);
    Page<NotificationLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
