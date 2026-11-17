package com.pnc.masters.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByUserIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long userId, LocalDateTime createdAt);

    long countByUserIdAndReadAtIsNullAndDeletedAtIsNullAndCreatedAtGreaterThanEqual(
            Long userId, LocalDateTime createdAt);

    Optional<Notification> findByNotificationIdAndUserId(Long notificationId, Long userId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Notification n
            SET n.deletedAt = :deletedAt
            WHERE n.deletedAt IS NULL AND n.createdAt < :cutoff
            """)
    int softDeleteOlderThan(@Param("cutoff") LocalDateTime cutoff, @Param("deletedAt") LocalDateTime deletedAt);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") LocalDateTime cutoff);
}
