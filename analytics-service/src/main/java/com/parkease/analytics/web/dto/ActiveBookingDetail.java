package com.parkease.analytics.web.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ActiveBookingDetail {
    private Long bookingId;
    private Long driverId;
    private String spotNumber;
    private LocalDateTime checkinAt;
    private Long estimatedDurationMinutes;  // null for CONFIRMED (not yet checked in)
}
