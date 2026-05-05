package com.parkease.auth.service.impl;

import com.parkease.auth.domain.entity.Vehicle;
import com.parkease.auth.repository.VehicleRepository;
import com.parkease.auth.web.dto.request.AddVehicleRequest;
import com.parkease.auth.web.dto.response.VehicleResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    @Test
    void addVehicle_ShouldSaveVehicle_WhenPlateIsNew() {
        AddVehicleRequest request = new AddVehicleRequest();
        request.setPlate("MH12AB1234");
        request.setVehicleType(Vehicle.VehicleType.SEDAN);

        when(vehicleRepository.existsByUserIdAndPlateIgnoreCase(anyLong(), anyString())).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> {
            Vehicle v = i.getArgument(0);
            v.setId(1L);
            return v;
        });

        VehicleResponse response = vehicleService.addVehicle(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getPlate()).isEqualTo("MH12AB1234");
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void addVehicle_ShouldThrow_WhenDuplicatePlate() {
        AddVehicleRequest request = new AddVehicleRequest();
        request.setPlate("MH12AB1234");

        when(vehicleRepository.existsByUserIdAndPlateIgnoreCase(anyLong(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.addVehicle(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void getMyVehicles_ShouldReturnList() {
        Vehicle vehicle = Vehicle.builder().id(1L).userId(1L).plate("KA01AA9999")
                .vehicleType(Vehicle.VehicleType.SUV).build();
        when(vehicleRepository.findByUserId(1L)).thenReturn(List.of(vehicle));

        List<VehicleResponse> result = vehicleService.getMyVehicles(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPlate()).isEqualTo("KA01AA9999");
    }

    @Test
    void deleteVehicle_ShouldCallDelete_WhenOwnerCalls() {
        Vehicle vehicle = Vehicle.builder().userId(1L).build();
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

        vehicleService.deleteVehicle(1L, 10L);

        verify(vehicleRepository).delete(vehicle);
    }

    @Test
    void deleteVehicle_ShouldThrow_WhenNotOwner() {
        Vehicle vehicle = Vehicle.builder().userId(2L).build();
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.deleteVehicle(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You do not own this vehicle");
    }

    @Test
    void deleteVehicle_ShouldThrow_WhenNotFound() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.deleteVehicle(1L, 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getByPlate_ShouldReturnVehicle_WhenFound() {
        Vehicle vehicle = Vehicle.builder().id(1L).plate("DL01AA1234")
                .vehicleType(Vehicle.VehicleType.SEDAN).build();
        when(vehicleRepository.findByPlateIgnoreCase("DL01AA1234")).thenReturn(Optional.of(vehicle));

        VehicleResponse result = vehicleService.getByPlate("DL01AA1234");

        assertThat(result.getPlate()).isEqualTo("DL01AA1234");
    }

    @Test
    void getByPlate_ShouldThrow_WhenNotFound() {
        when(vehicleRepository.findByPlateIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getByPlate("UNKNOWN"))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
