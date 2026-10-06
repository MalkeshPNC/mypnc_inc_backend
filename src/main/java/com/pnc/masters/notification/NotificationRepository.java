package com.pnc.masters.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);

    Optional<Notification> findByNotificationIdAndUserId(Long notificationId, Long userId);

    List<Notification> findByUserIdAndReadAtIsNull(Long userId);
}
