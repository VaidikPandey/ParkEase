package com.parkease.notification.repository;

import com.parkease.notification.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderBySentAtDesc(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndIsReadOrderBySentAtDesc(Long recipientId, Boolean isRead, Pageable pageable);

    long countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    List<Notification> findByType(Notification.NotificationType type);

    List<Notification> findByRelatedId(Long relatedId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.notificationId = :id")
    void markAsReadById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :recipientId")
    void markAllReadByRecipient(@Param("recipientId") Long recipientId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.notificationId = :id")
    void deleteByNotificationId(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipientId = :recipientId")
    void deleteAllByRecipientId(@Param("recipientId") Long recipientId);
}
