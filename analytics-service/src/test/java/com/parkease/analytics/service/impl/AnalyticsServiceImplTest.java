package com.parkease.analytics.service.impl;

import com.parkease.analytics.domain.entity.BookingAnalytics;
import com.parkease.analytics.repository.BookingAnalyticsRepository;
import com.parkease.analytics.web.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private BookingAnalyticsRepository repository;

    @Mock
    private WebClient paymentWebClient;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    void getUtilisation_ShouldCalculatePercentagesCorrectly() {
        Long lotId = 1L;
        LocalDateTime now = LocalDateTime.now();
        List<Object[]> mockRows = List.of(
                new Object[]{"PRE_BOOKING", 75L},
                new Object[]{"WALK_IN", 25L}
        );

        when(repository.countByBookingType(anyLong(), any(), any())).thenReturn(mockRows);

        var response = analyticsService.getUtilisation(lotId, now, now);

        assertThat(response.getTotal()).isEqualTo(100);
        assertThat(response.getPreBookingPct()).isEqualTo(75.0);
        assertThat(response.getWalkInPct()).isEqualTo(25.0);
    }

    @Test
    void getUtilisation_ShouldReturnZeroPct_WhenNoBookings() {
        when(repository.countByBookingType(anyLong(), any(), any())).thenReturn(List.of());

        var response = analyticsService.getUtilisation(1L, LocalDateTime.now(), LocalDateTime.now());

        assertThat(response.getTotal()).isEqualTo(0);
        assertThat(response.getPreBookingPct()).isEqualTo(0.0);
        assertThat(response.getWalkInPct()).isEqualTo(0.0);
    }

    @Test
    void getOccupancy_ShouldReturnStats_WhenCompletedBookingsExist() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to   = LocalDateTime.now();

        BookingAnalytics b1 = new BookingAnalytics();
        b1.setDurationMinutes(60L);
        BookingAnalytics b2 = new BookingAnalytics();
        b2.setDurationMinutes(120L);

        when(repository.findCompletedInRange(anyLong(), any(), any())).thenReturn(List.of(b1, b2));

        OccupancyResponse response = analyticsService.getOccupancy(1L, from, to);

        assertThat(response.getTotalCompletedBookings()).isEqualTo(2L);
        assertThat(response.getTotalBookingMinutes()).isEqualTo(180L);
        assertThat(response.getAverageBookingDurationMinutes()).isEqualTo(90.0);
    }

    @Test
    void getOccupancy_ShouldReturnZeroAvg_WhenNoCompletedBookings() {
        when(repository.findCompletedInRange(anyLong(), any(), any())).thenReturn(List.of());

        OccupancyResponse response = analyticsService.getOccupancy(1L, LocalDateTime.now(), LocalDateTime.now());

        assertThat(response.getTotalCompletedBookings()).isEqualTo(0L);
        assertThat(response.getAverageBookingDurationMinutes()).isEqualTo(0.0);
    }

    @Test
    void getActiveBookings_ShouldReturnConfirmedAndActiveCount() {
        BookingAnalytics confirmed = new BookingAnalytics();
        confirmed.setStatus(BookingAnalytics.BookingStatus.CONFIRMED);
        confirmed.setBookingId(1L);

        BookingAnalytics active = new BookingAnalytics();
        active.setStatus(BookingAnalytics.BookingStatus.ACTIVE);
        active.setBookingId(2L);
        active.setCheckinAt(LocalDateTime.now().minusMinutes(30));

        when(repository.findByLotIdAndStatusIn(anyLong(), any())).thenReturn(List.of(confirmed, active));

        ActiveBookingsResponse response = analyticsService.getActiveBookings(1L);

        assertThat(response.getTotal()).isEqualTo(2);
        assertThat(response.getConfirmedPending()).isEqualTo(1);
        assertThat(response.getCurrentlyParked()).isEqualTo(1);
    }

    @Test
    void getHourlyTraffic_ShouldMapRowsToResponse() {
        List<Object[]> rows = List.of(
                new Object[]{8, 10L},
                new Object[]{9, 20L}
        );

        when(repository.countByHour(anyLong(), any(), any())).thenReturn(rows);

        List<HourlyTrafficResponse> result = analyticsService.getHourlyTraffic(1L, LocalDateTime.now(), LocalDateTime.now());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getHour()).isEqualTo(8);
        assertThat(result.get(0).getBookingCount()).isEqualTo(10L);
    }

    @Test
    void getLotSummary_ShouldReturnCombinedStats() {
        when(repository.findByLotIdAndStatusIn(anyLong(), any())).thenReturn(List.of());
        when(repository.countByBookingType(anyLong(), any(), any())).thenReturn(List.of());
        when(repository.countByHour(anyLong(), any(), any())).thenReturn(List.of());

        var response = analyticsService.getLotSummary(1L);

        assertThat(response).isNotNull();
        assertThat(response.getLotId()).isEqualTo(1L);
    }

    @Test
    void getDriverAnalytics_ShouldSummariseCounts() {
        BookingAnalytics completed = new BookingAnalytics();
        completed.setStatus(BookingAnalytics.BookingStatus.COMPLETED);
        completed.setDurationMinutes(60L);
        completed.setTotalFare(50.0);

        BookingAnalytics cancelled = new BookingAnalytics();
        cancelled.setStatus(BookingAnalytics.BookingStatus.CANCELLED);

        when(repository.findByDriverIdInRange(anyLong(), any(), any())).thenReturn(List.of(completed, cancelled));
        java.util.List<Object[]> driverTypeRows = new java.util.ArrayList<>();
        driverTypeRows.add(new Object[]{"PRE_BOOKING", 1L});
        when(repository.countByBookingTypeForDriver(anyLong(), any(), any()))
                .thenReturn(driverTypeRows);

        DriverAnalyticsResponse response = analyticsService.getDriverAnalytics(1L, LocalDateTime.now(), LocalDateTime.now());

        assertThat(response.getTotalBookings()).isEqualTo(2L);
        assertThat(response.getCompletedBookings()).isEqualTo(1L);
        assertThat(response.getCancelledBookings()).isEqualTo(1L);
        assertThat(response.getTotalSpent()).isEqualTo(50.0);
    }
}
