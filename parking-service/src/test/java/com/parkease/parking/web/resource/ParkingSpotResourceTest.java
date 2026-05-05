package com.parkease.parking.web.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.parking.domain.entity.ParkingSpot;
import com.parkease.parking.service.ParkingSpotService;
import com.parkease.parking.web.dto.request.CreateSpotRequest;
import com.parkease.parking.web.dto.response.ParkingSpotResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingSpotResource.class)
@AutoConfigureMockMvc(addFilters = false)
class ParkingSpotResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParkingSpotService spotService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getSpotsByLot_ShouldReturn200() throws Exception {
        when(spotService.getSpotsByLot(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/parking/lots/1/spots"))
                .andExpect(status().isOk());
    }

    @Test
    void getAvailableSpots_ShouldReturn200() throws Exception {
        when(spotService.getAvailableSpots(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/parking/lots/1/spots/available"))
                .andExpect(status().isOk());
    }

    @Test
    void getSpotsByType_ShouldReturn200() throws Exception {
        when(spotService.getSpotsByType(anyLong(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/parking/lots/1/spots/type/STANDARD"))
                .andExpect(status().isOk());
    }

    @Test
    void getSpotById_ShouldReturn200() throws Exception {
        ParkingSpotResponse response = ParkingSpotResponse.builder()
                .spotId(1L).spotNumber("A1").spotType(ParkingSpot.SpotType.STANDARD.name())
                .status(ParkingSpot.SpotStatus.AVAILABLE.name()).build();
        when(spotService.getSpotById(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/v1/parking/spots/1"))
                .andExpect(status().isOk());
    }

    @Test
    void addSpot_ShouldReturn201_WhenManagerAdds() throws Exception {
        CreateSpotRequest request = new CreateSpotRequest();
        request.setSpotNumber("A1");
        request.setFloor(1);
        request.setSpotType(ParkingSpot.SpotType.STANDARD);
        request.setPricePerHour(5.0);

        ParkingSpotResponse response = ParkingSpotResponse.builder()
                .spotId(1L).spotNumber("A1").spotType(ParkingSpot.SpotType.STANDARD.name())
                .status(ParkingSpot.SpotStatus.AVAILABLE.name()).build();
        when(spotService.addSpot(anyLong(), anyLong(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/parking/manager/lots/1/spots")
                        .header("X-User-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteSpot_ShouldReturn204() throws Exception {
        doNothing().when(spotService).deleteSpot(anyLong(), anyLong());

        mockMvc.perform(delete("/api/v1/parking/manager/spots/1")
                        .header("X-User-Id", "10"))
                .andExpect(status().isNoContent());
    }
}
