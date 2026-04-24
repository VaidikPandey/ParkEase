package com.parkease.analytics.service.impl;

import com.parkease.analytics.domain.entity.BookingAnalytics;
import com.parkease.analytics.repository.BookingAnalyticsRepository;
import com.parkease.analytics.service.AnalyticsService;
import com.parkease.analytics.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final BookingAnalyticsRepository repository;
    private final RestTemplate restTemplate;

    @Value("${services.payment-url}")
    private String paymentServiceUrl;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public OccupancyResponse getOccupancy(Long lotId, LocalDateTime from, LocalDateTime to) {
        List<BookingAnalytics> completed = repository.findCompletedInRange(lotId, from, to);

        long totalMinutes = completed.stream()
                .filter(b -> b.getDurationMinutes() != null)
                .mapToLong(BookingAnalytics::getDurationMinutes)
                .sum();

        double avgDuration = completed.isEmpty() ? 0.0
                : (double) totalMinutes / completed.size();

        return OccupancyResponse.builder()
                .lotId(lotId)
                .from(from.format(FMT))
                .to(to.format(FMT))
                .totalCompletedBookings((long) completed.size())
                .totalBookingMinutes(totalMinutes)
                .averageBookingDurationMinutes(Math.round(avgDuration * 100.0) / 100.0)
                .build();
    }

    @Override
    public ActiveBookingsResponse getActiveBookings(Long lotId) {
        List<BookingAnalytics> records = repository.findByLotIdAndStatusIn(
                lotId,
                List.of(BookingAnalytics.BookingStatus.CONFIRMED, BookingAnalytics.BookingStatus.ACTIVE)
        );

        LocalDateTime now = LocalDateTime.now();
        int confirmed = 0, active = 0;

        List<ActiveBookingDetail> details = records.stream().map(b -> {
            Long estimatedMinutes = null;
            if (b.getStatus() == BookingAnalytics.BookingStatus.ACTIVE && b.getCheckinAt() != null) {
                estimatedMinutes = java.time.Duration.between(b.getCheckinAt(), now).toMinutes();
            }
            return ActiveBookingDetail.builder()
                    .bookingId(b.getBookingId())
                    .driverId(b.getDriverId())
                    .spotNumber(b.getSpotNumber())
                    .checkinAt(b.getCheckinAt())
                    .estimatedDurationMinutes(estimatedMinutes)
                    .build();
        }).toList();

        for (BookingAnalytics b : records) {
            if (b.getStatus() == BookingAnalytics.BookingStatus.CONFIRMED) confirmed++;
            else active++;
        }

        return ActiveBookingsResponse.builder()
                .lotId(lotId)
                .confirmedPending(confirmed)
                .currentlyParked(active)
                .total(records.size())
                .activeBookings(details)
                .build();
    }

    @Override
    public UtilisationResponse getUtilisation(Long lotId, LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = repository.countByBookingType(lotId, from, to);

        long preBooking = 0, walkIn = 0;
        for (Object[] row : rows) {
            String type  = (String) row[0];
            long   count = ((Number) row[1]).longValue();
            if ("PRE_BOOKING".equals(type)) preBooking = count;
            else if ("WALK_IN".equals(type)) walkIn = count;
        }

        long total = preBooking + walkIn;
        double prePct  = total == 0 ? 0.0 : Math.round(preBooking * 10000.0 / total) / 100.0;
        double walkPct = total == 0 ? 0.0 : Math.round(walkIn    * 10000.0 / total) / 100.0;

        return UtilisationResponse.builder()
                .lotId(lotId)
                .from(from.format(FMT))
                .to(to.format(FMT))
                .preBookingCount(preBooking)
                .walkInCount(walkIn)
                .total(total)
                .preBookingPct(prePct)
                .walkInPct(walkPct)
                .build();
    }

    @Override
    public List<HourlyTrafficResponse> getHourlyTraffic(
            Long lotId, LocalDateTime from, LocalDateTime to) {

        return repository.countByHour(lotId, from, to).stream()
                .map(row -> HourlyTrafficResponse.builder()
                        .hour(((Number) row[0]).intValue())
                        .bookingCount(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    @Override
    public Object getRevenue(Long lotId, LocalDateTime from, LocalDateTime to, String authHeader) {
        String url = paymentServiceUrl + "/api/v1/payments/revenue/lot/" + lotId
                + "?from=" + from.format(FMT)
                + "&to=" + to.format(FMT);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch revenue from payment-service for lot {}: {}", lotId, e.getMessage());
            throw new IllegalStateException("Revenue data unavailable: " + e.getMessage());
        }
    }

    @Override
    public LotSummaryResponse getLotSummary(Long lotId) {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to   = LocalDateTime.now();

        return LotSummaryResponse.builder()
                .lotId(lotId)
                .activeBookings(getActiveBookings(lotId))
                .utilisation(getUtilisation(lotId, from, to))
                .peakHours(getHourlyTraffic(lotId, from, to))
                .build();
    }
}
