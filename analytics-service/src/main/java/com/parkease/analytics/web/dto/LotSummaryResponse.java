package com.parkease.analytics.web.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class LotSummaryResponse {
    private Long lotId;
    private ActiveBookingsResponse activeBookings;
    private UtilisationResponse utilisation;
    private List<HourlyTrafficResponse> peakHours;
}
