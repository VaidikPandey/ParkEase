package com.parkease.analytics.web.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OccupancyResponse {
    private Long lotId;
    private String from;
    private String to;
    private Long totalCompletedBookings;
    private Long totalBookingMinutes;
    private Double averageBookingDurationMinutes;
}
