package com.parkease.notification.web.dto;

import com.parkease.notification.domain.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BulkNotificationRequest {

    @NotEmpty(message = "Recipient IDs are required")
    private List<Long> recipientIds;

    @NotNull(message = "Type is required")
    private Notification.NotificationType type;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "Channel is required")
    private Notification.NotificationChannel channel;
}
