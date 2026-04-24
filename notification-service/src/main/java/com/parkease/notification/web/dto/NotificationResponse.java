package com.parkease.notification.web.dto;

import com.parkease.notification.domain.entity.Notification;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long notificationId;
    private Long recipientId;
    private Notification.NotificationType type;
    private String title;
    private String message;
    private Notification.NotificationChannel channel;
    private Long relatedId;
    private String relatedType;
    private Boolean isRead;
    private LocalDateTime sentAt;
}
