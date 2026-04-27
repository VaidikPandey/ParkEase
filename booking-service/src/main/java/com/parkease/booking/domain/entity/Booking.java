package com.parkease.booking.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_driver", columnList = "driver_id"),
        @Index(name = "idx_booking_spot", columnList = "spot_id"),
        @Index(name = "idx_booking_status", columnList = "status"),
        @Index(name = "idx_booking_lot", columnList = "lot_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @Column(nullable = false)
    private Long driverId;

    @Column(nullable = false)
    private Long spotId;

    @Column(nullable = false)
    private Long lotId;

    @Column(nullable = false)
    private String spotNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING) // telling jpa how to store enum value in the db.
    private BookingType bookingType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column
    private LocalDateTime checkInTime;

    @Column
    private LocalDateTime checkOutTime;

    @Column(nullable = false)
    private Double pricePerHour;

    @Column
    private Double totalFare;

    @Column
    private String vehiclePlate;

    @Column
    private String driverEmail;

    @Column
    private String cancellationReason;

    @Column(nullable = false)
    @Builder.Default
    private boolean reminderSent = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum BookingType {
        PRE_BOOKING,   // advance reservation
        WALK_IN        // immediate, no advance payment
    }

    public enum BookingStatus {
        PENDING,       // created, awaiting check-in
        CONFIRMED,     // payment done for pre-booking
        CHECKED_IN,    // driver has arrived
        CHECKED_OUT,   // driver has left, fare calculated
        CANCELLED,     // cancelled by driver or auto-cancelled
        EXPIRED        // no check-in within grace period
    }
}