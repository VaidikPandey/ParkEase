package com.parkease.auth.web.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.auth.service.VehicleService;
import com.parkease.auth.web.dto.request.AddVehicleRequest;
import com.parkease.auth.web.dto.response.VehicleResponse;
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

@WebMvcTest(VehicleResource.class)
@AutoConfigureMockMvc(addFilters = false)
class VehicleResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VehicleService vehicleService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getMyVehicles_ShouldReturn200() throws Exception {
        when(vehicleService.getMyVehicles(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/vehicles")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void addVehicle_ShouldReturn201() throws Exception {
        AddVehicleRequest request = new AddVehicleRequest();
        request.setPlate("MH12AB1234");
        request.setVehicleType(com.parkease.auth.domain.entity.Vehicle.VehicleType.SEDAN);

        when(vehicleService.addVehicle(anyLong(), any())).thenReturn(VehicleResponse.builder().build());

        mockMvc.perform(post("/api/v1/users/vehicles")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteVehicle_ShouldReturn204() throws Exception {
        doNothing().when(vehicleService).deleteVehicle(anyLong(), anyLong());

        mockMvc.perform(delete("/api/v1/users/vehicles/1")
                .header("X-User-Id", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getByPlate_ShouldReturn200() throws Exception {
        when(vehicleService.getByPlate(anyString())).thenReturn(VehicleResponse.builder().build());

        mockMvc.perform(get("/api/v1/users/vehicles/plate/ABC123"))
                .andExpect(status().isOk());
    }
}
