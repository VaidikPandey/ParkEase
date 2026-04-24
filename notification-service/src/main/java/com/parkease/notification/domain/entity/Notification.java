package com.parkease.notification.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_recipient", columnList = "recipientId"),
        @Index(name = "idx_notif_is_read",   columnList = "isRead"),
        @Index(name = "idx_notif_type",      columnList = "type"),
        @Index(name = "idx_notif_related",   columnList = "relatedId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @Column(nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationChannel channel;

    private Long relatedId;

    @Column(length = 50)
    private String relatedType;

    @Column(nullable = false)
    private Boolean isRead = false;

    private LocalDateTime sentAt;

    @PrePersist
    public void prePersist() {
        if (this.sentAt == null) this.sentAt = LocalDateTime.now();
        if (this.isRead == null) this.isRead = false;
    }

    public enum NotificationType {
        BOOKING, CHECKIN, CHECKOUT, EXPIRY, PAYMENT, REFUND, SYSTEM
    }

    public enum NotificationChannel {
        APP, EMAIL, SMS
    }
}
