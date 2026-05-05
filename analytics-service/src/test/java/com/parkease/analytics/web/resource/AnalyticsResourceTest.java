package com.parkease.analytics.web.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.analytics.service.AnalyticsService;
import com.parkease.analytics.web.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsResource.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getOccupancy_ShouldReturn200() throws Exception {
        when(analyticsService.getOccupancy(anyLong(), any(), any())).thenReturn(OccupancyResponse.builder().build());

        mockMvc.perform(get("/api/v1/analytics/occupancy")
                .param("lotId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void getActiveBookings_ShouldReturn200() throws Exception {
        when(analyticsService.getActiveBookings(anyLong())).thenReturn(ActiveBookingsResponse.builder().build());

        mockMvc.perform(get("/api/v1/analytics/active")
                .param("lotId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void getUtilisation_ShouldReturn200() throws Exception {
        when(analyticsService.getUtilisation(anyLong(), any(), any())).thenReturn(UtilisationResponse.builder().build());

        mockMvc.perform(get("/api/v1/analytics/utilisation")
                .param("lotId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void getHourlyTraffic_ShouldReturn200() throws Exception {
        when(analyticsService.getHourlyTraffic(anyLong(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/analytics/traffic/hourly")
                .param("lotId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void getRevenue_ShouldReturn200() throws Exception {
        when(analyticsService.getRevenue(anyLong(), any(), any(), anyLong(), anyString())).thenReturn(java.util.Map.of("total", 0.0));

        mockMvc.perform(get("/api/v1/analytics/revenue")
                .param("lotId", "1")
                .header("X-User-Id", "1")
                .header("X-User-Role", "MANAGER"))
                .andExpect(status().isOk());
    }

    @Test
    void getLotSummary_ShouldReturn200() throws Exception {
        when(analyticsService.getLotSummary(anyLong())).thenReturn(LotSummaryResponse.builder().build());

        mockMvc.perform(get("/api/v1/analytics/summary")
                .param("lotId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyAnalytics_ShouldReturn200() throws Exception {
        when(analyticsService.getDriverAnalytics(anyLong(), any(), any())).thenReturn(DriverAnalyticsResponse.builder().build());

        mockMvc.perform(get("/api/v1/analytics/my")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }
}
