package com.parkease.parking.web.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.parking.service.ParkingLotService;
import com.parkease.parking.web.dto.request.CreateLotRequest;
import com.parkease.parking.web.dto.response.ParkingLotResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingLotResource.class)
@AutoConfigureMockMvc(addFilters = false)
class ParkingLotResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParkingLotService lotService;

    @MockBean
    private com.parkease.parking.service.AvailabilityCounterService counterService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getApprovedLots_ShouldReturn200() throws Exception {
        when(lotService.getApprovedLots()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/parking/lots/search"))
                .andExpect(status().isOk());
    }

    @Test
    void createLot_ShouldReturn201() throws Exception {
        CreateLotRequest request = new CreateLotRequest();
        request.setName("Test Lot");
        request.setAddress("Addr");
        request.setCity("City");
        request.setLatitude(0.0);
        request.setLongitude(0.0);
        request.setOpeningTime("09:00");
        request.setClosingTime("21:00");
        request.setMaxCapacity(10);

        when(lotService.createLot(anyLong(), any())).thenReturn(ParkingLotResponse.builder().build());

        mockMvc.perform(post("/api/v1/parking/manager/lots")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void getLotById_ShouldReturn200() throws Exception {
        when(lotService.getLotById(anyLong())).thenReturn(ParkingLotResponse.builder().build());

        mockMvc.perform(get("/api/v1/parking/lots/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getAvailability_ShouldReturn200() throws Exception {
        when(counterService.getAvailableCount(anyLong())).thenReturn(5);
        when(counterService.isAvailable(anyLong())).thenReturn(true);

        mockMvc.perform(get("/api/v1/parking/lots/1/availability"))
                .andExpect(status().isOk());
    }

    @Test
    void toggleStatus_ShouldReturn200() throws Exception {
        when(lotService.toggleLotStatus(anyLong(), anyLong()))
                .thenReturn(ParkingLotResponse.builder().build());

        mockMvc.perform(put("/api/v1/parking/manager/lots/1/toggle")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }
}
