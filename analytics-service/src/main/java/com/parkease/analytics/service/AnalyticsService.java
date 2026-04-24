package com.parkease.analytics.service;

import com.parkease.analytics.web.dto.*;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsService {

    OccupancyResponse getOccupancy(Long lotId, LocalDateTime from, LocalDateTime to);

    ActiveBookingsResponse getActiveBookings(Long lotId);

    UtilisationResponse getUtilisation(Long lotId, LocalDateTime from, LocalDateTime to);

    List<HourlyTrafficResponse> getHourlyTraffic(Long lotId, LocalDateTime from, LocalDateTime to);

    Object getRevenue(Long lotId, LocalDateTime from, LocalDateTime to, String authHeader);

    LotSummaryResponse getLotSummary(Long lotId);
}
