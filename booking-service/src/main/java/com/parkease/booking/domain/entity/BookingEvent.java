package com.parkease.booking.domain.entity;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingEvent {

    private Long bookingId;
    private Long driverId;
    private Long spotId;
    private Long lotId;
    private String spotNumber;
    private String driverEmail;
    private String vehiclePlate;
    private String eventType;          // BOOKING_CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Double totalFare;
    private LocalDateTime occurredAt;
}

// This is not a database entity. There's no @Entity annotation.
// It's a plain Java object that gets serialized to JSON and published to RabbitMQ.