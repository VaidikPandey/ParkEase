package com.parkease.notification.web.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingEvent {
    private Long bookingId;
    private Long driverId;
    private Long spotId;
    private Long lotId;
    private String spotNumber;
    private String driverEmail;
    private String vehiclePlate;
    private String eventType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Double totalFare;
    private LocalDateTime occurredAt;
}
