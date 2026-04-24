package com.parkease.analytics.web.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UtilisationResponse {
    private Long lotId;
    private String from;
    private String to;
    private Long preBookingCount;
    private Long walkInCount;
    private Long total;
    private Double preBookingPct;
    private Double walkInPct;
}
