package com.parkease.analytics.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_analytics", indexes = {
        @Index(name = "idx_ba_lot_id",    columnList = "lotId"),
        @Index(name = "idx_ba_driver",    columnList = "driverId"),
        @Index(name = "idx_ba_status",    columnList = "status"),
        @Index(name = "idx_ba_confirmed", columnList = "confirmedAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Long driverId;

    private Long spotId;
    private Long lotId;
    private String spotNumber;

    @Column(length = 20)
    private String bookingType;   // PRE_BOOKING or WALK_IN

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    private LocalDateTime confirmedAt;
    private LocalDateTime checkinAt;
    private LocalDateTime checkoutAt;
    private LocalDateTime cancelledAt;

    private Double totalFare;
    private Long durationMinutes;   // actual duration set on checkout

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum BookingStatus {
        CONFIRMED, ACTIVE, COMPLETED, CANCELLED, EXPIRED
    }
}
