package com.parkease.notification.service;

import com.parkease.notification.domain.entity.Notification;
import com.parkease.notification.web.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    NotificationResponse send(NotificationRequest request);
    List<NotificationResponse> sendBulk(BulkNotificationRequest request);
    void markAsRead(Long notificationId, Long callerId, boolean isAdmin);
    void markAllRead(Long recipientId);
    Page<NotificationResponse> getByRecipient(Long recipientId, Pageable pageable);
    Page<NotificationResponse> getUnread(Long recipientId, Pageable pageable);
    long getUnreadCount(Long recipientId);
    void deleteNotification(Long notificationId, Long callerId, boolean isAdmin);
    void deleteAllByRecipient(Long recipientId);
    List<NotificationResponse> getAll();
    void sendEmail(String to, String subject, String body);
    void sendHtmlEmail(String to, String subject, String htmlBody);
    NotificationResponse createFromEvent(Long recipientId, Notification.NotificationType type,
                                         String title, String message, Long relatedId, String relatedType);
}
