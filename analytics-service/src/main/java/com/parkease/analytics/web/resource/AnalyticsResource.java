package com.parkease.analytics.web.resource;

import com.parkease.analytics.service.AnalyticsService;
import com.parkease.analytics.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Parking occupancy, utilisation, traffic, and revenue analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsResource {

    private final AnalyticsService analyticsService;

    @GetMapping("/occupancy")
    @Operation(summary = "Occupancy for a lot in a date range (defaults to last 30 days)")
    public ResponseEntity<OccupancyResponse> getOccupancy(
            @RequestParam Long lotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime resolvedTo   = to   != null ? to   : LocalDateTime.now();
        LocalDateTime resolvedFrom = from != null ? from : resolvedTo.minusDays(30);
        return ResponseEntity.ok(analyticsService.getOccupancy(lotId, resolvedFrom, resolvedTo));
    }

    @GetMapping("/active")
    @Operation(summary = "Current active and pending bookings for a lot")
    public ResponseEntity<ActiveBookingsResponse> getActiveBookings(
            @RequestParam Long lotId) {
        return ResponseEntity.ok(analyticsService.getActiveBookings(lotId));
    }

    @GetMapping("/utilisation")
    @Operation(summary = "PRE_BOOKING vs WALK_IN utilisation breakdown for a lot (defaults to last 30 days)")
    public ResponseEntity<UtilisationResponse> getUtilisation(
            @RequestParam Long lotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime resolvedTo   = to   != null ? to   : LocalDateTime.now();
        LocalDateTime resolvedFrom = from != null ? from : resolvedTo.minusDays(30);
        return ResponseEntity.ok(analyticsService.getUtilisation(lotId, resolvedFrom, resolvedTo));
    }

    @GetMapping("/traffic/hourly")
    @Operation(summary = "Bookings per hour of day for a lot in a date range (defaults to last 30 days)")
    public ResponseEntity<List<HourlyTrafficResponse>> getHourlyTraffic(
            @RequestParam Long lotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime resolvedTo   = to   != null ? to   : LocalDateTime.now();
        LocalDateTime resolvedFrom = from != null ? from : resolvedTo.minusDays(30);
        return ResponseEntity.ok(analyticsService.getHourlyTraffic(lotId, resolvedFrom, resolvedTo));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Revenue for a lot in a date range (defaults to last 30 days, proxied from payment-service)")
    public ResponseEntity<Object> getRevenue(
            @RequestParam Long lotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestHeader("Authorization") String authHeader) {
        LocalDateTime resolvedTo   = to   != null ? to   : LocalDateTime.now();
        LocalDateTime resolvedFrom = from != null ? from : resolvedTo.minusDays(30);
        return ResponseEntity.ok(analyticsService.getRevenue(lotId, resolvedFrom, resolvedTo, authHeader));
    }

    @GetMapping("/summary")
    @Operation(summary = "30-day summary for a lot (active bookings + utilisation + peak hours)")
    public ResponseEntity<LotSummaryResponse> getLotSummary(
            @RequestParam Long lotId) {
        return ResponseEntity.ok(analyticsService.getLotSummary(lotId));
    }
}
