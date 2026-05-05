package com.parkease.parking.service.impl;

import com.parkease.parking.domain.entity.ParkingLot;
import com.parkease.parking.domain.entity.ParkingSpot;
import com.parkease.parking.repository.ParkingLotRepository;
import com.parkease.parking.repository.ParkingSpotRepository;
import com.parkease.parking.service.AvailabilityCounterService;
import com.parkease.parking.web.dto.request.CreateSpotRequest;
import com.parkease.parking.web.dto.request.BulkCreateSpotRequest;
import com.parkease.parking.web.dto.response.ParkingSpotResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingSpotServiceImplTest {

    @Mock
    private ParkingSpotRepository spotRepository;
    @Mock
    private ParkingLotRepository lotRepository;
    @Mock
    private AvailabilityCounterService counterService;

    @InjectMocks
    private ParkingSpotServiceImpl spotService;

    @Test
    void addSpot_ShouldCreateSpot_WhenOwnerCalls() {
        Long lotId = 1L;
        Long managerId = 10L;
        ParkingLot lot = ParkingLot.builder()
                .lotId(lotId)
                .managerId(managerId)
                .totalSpots(0)
                .maxCapacity(10)
                .build();
        CreateSpotRequest request = new CreateSpotRequest();
        request.setSpotNumber("A1");
        request.setFloor(1);
        request.setSpotType(ParkingSpot.SpotType.STANDARD);

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(lot));
        when(spotRepository.existsByParkingLot_LotIdAndSpotNumber(anyLong(), anyString())).thenReturn(false);

        spotService.addSpot(lotId, managerId, request);

        verify(spotRepository).save(any(ParkingSpot.class));
        verify(counterService).increment(lotId);
    }

    @Test
    void deleteSpot_ShouldCallDelete_WhenOwnerCalls() {
        Long spotId = 5L;
        Long managerId = 10L;
        ParkingLot lot = ParkingLot.builder().lotId(1L).managerId(managerId).totalSpots(2).build();
        ParkingSpot spot = ParkingSpot.builder()
                .spotId(spotId)
                .parkingLot(lot)
                .status(ParkingSpot.SpotStatus.AVAILABLE)
                .build();

        when(spotRepository.findById(spotId)).thenReturn(Optional.of(spot));

        spotService.deleteSpot(spotId, managerId);

        verify(spotRepository).delete(spot);
        verify(counterService).decrement(1L);
    }

    @Test
    void getAvailableSpots_ShouldReturnOnlyAvailableSpots() {
        ParkingLot lot = ParkingLot.builder().lotId(1L).build();
        ParkingSpot spot = ParkingSpot.builder()
                .spotId(1L)
                .status(ParkingSpot.SpotStatus.AVAILABLE)
                .spotType(ParkingSpot.SpotType.STANDARD)
                .parkingLot(lot)
                .build();
        when(spotRepository.findByParkingLot_LotIdAndStatus(1L, ParkingSpot.SpotStatus.AVAILABLE))
                .thenReturn(List.of(spot));

        List<ParkingSpotResponse> result = spotService.getAvailableSpots(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getSpotsByLot_ShouldReturnAllSpots() {
        ParkingLot lot = ParkingLot.builder().lotId(1L).build();
        ParkingSpot spot = ParkingSpot.builder()
                .spotId(1L)
                .parkingLot(lot)
                .spotType(ParkingSpot.SpotType.STANDARD)
                .status(ParkingSpot.SpotStatus.AVAILABLE)
                .build();
        when(spotRepository.findByParkingLot_LotId(1L)).thenReturn(List.of(spot));

        List<ParkingSpotResponse> result = spotService.getSpotsByLot(1L);

        assertThat(result).hasSize(1);
    }
}
