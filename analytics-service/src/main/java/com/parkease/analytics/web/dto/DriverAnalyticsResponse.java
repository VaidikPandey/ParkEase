package com.parkease.analytics.web.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DriverAnalyticsResponse {
    private Long   driverId;
    private String from;
    private String to;
    private Long   totalBookings;
    private Long   completedBookings;
    private Long   cancelledBookings;
    private Long   preBookingCount;
    private Long   walkInCount;
    private Double preBookingPct;
    private Double walkInPct;
    private Long   totalDurationMinutes;
    private Double totalSpent;
}
