package com.parkease.parking.service.impl;

import com.parkease.parking.domain.entity.ParkingLot;
import com.parkease.parking.repository.ParkingLotRepository;
import com.parkease.parking.repository.ParkingSpotRepository;
import com.parkease.parking.service.AvailabilityCounterService;
import com.parkease.parking.web.dto.request.CreateLotRequest;
import com.parkease.parking.web.dto.request.UpdateLotRequest;
import com.parkease.parking.web.dto.response.ParkingLotResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingLotServiceImplTest {

    @Mock
    private ParkingLotRepository lotRepository;
    @Mock
    private ParkingSpotRepository spotRepository;
    @Mock
    private AvailabilityCounterService counterService;

    @InjectMocks
    private ParkingLotServiceImpl lotService;

    @Test
    void createLot_ShouldSaveLotAndInitCounter() {
        // Arrange
        Long managerId = 1L;
        CreateLotRequest request = new CreateLotRequest();
        request.setName("Downtown Lot");
        request.setAddress("123 Main St");
        request.setCity("Pune");
        request.setLatitude(18.5204);
        request.setLongitude(73.8567);
        request.setOpeningTime("08:00");
        request.setClosingTime("22:00");
        request.setMaxCapacity(50);

        when(lotRepository.save(any(ParkingLot.class))).thenAnswer(invocation -> {
            ParkingLot saved = invocation.getArgument(0);
            return saved;
        });

        // Act
        ParkingLotResponse response = lotService.createLot(managerId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Downtown Lot");
        assertThat(response.getStatus()).isEqualTo(ParkingLot.LotStatus.PENDING.name());
        verify(lotRepository).save(any(ParkingLot.class));
        verify(counterService).initCounter(any(), eq(0));
    }

    @Test
    void updateLot_ShouldUpdateFields_WhenOwnerCalls() {
        // Arrange
        Long lotId = 1L;
        Long managerId = 10L;
        ParkingLot lot = ParkingLot.builder()
                .lotId(lotId)
                .managerId(managerId)
                .name("Old Name")
                .status(ParkingLot.LotStatus.APPROVED)
                .build();

        UpdateLotRequest request = new UpdateLotRequest();
        request.setName("New Name");

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(lot));
        when(lotRepository.save(any(ParkingLot.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        ParkingLotResponse response = lotService.updateLot(lotId, managerId, request);

        // Assert
        assertThat(response.getName()).isEqualTo("New Name");
        verify(lotRepository).save(lot);
    }

    @Test
    void toggleLotStatus_ShouldSwitchBetweenApprovedAndClosed() {
        // Arrange
        Long lotId = 1L;
        Long managerId = 10L;
        ParkingLot lot = ParkingLot.builder()
                .lotId(lotId)
                .managerId(managerId)
                .status(ParkingLot.LotStatus.APPROVED)
                .build();

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(lot));

        // Act & Assert (APPROVED -> CLOSED)
        lotService.toggleLotStatus(lotId, managerId);
        assertThat(lot.getStatus()).isEqualTo(ParkingLot.LotStatus.CLOSED);

        // Act & Assert (CLOSED -> APPROVED)
        lotService.toggleLotStatus(lotId, managerId);
        assertThat(lot.getStatus()).isEqualTo(ParkingLot.LotStatus.APPROVED);
    }

    @Test
    void getLotsByManager_ShouldReturnLots() {
        ParkingLot lot = ParkingLot.builder().lotId(1L).managerId(10L).status(ParkingLot.LotStatus.APPROVED).build();
        when(lotRepository.findByManagerId(10L)).thenReturn(List.of(lot));
        when(counterService.getAvailableCount(anyLong())).thenReturn(0);

        List<ParkingLotResponse> result = lotService.getLotsByManager(10L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getLotsByCity_ShouldReturnMatchingLots() {
        ParkingLot lot = ParkingLot.builder().lotId(1L).city("Pune").status(ParkingLot.LotStatus.APPROVED).build();
        when(lotRepository.findByCityAndStatus("Pune", ParkingLot.LotStatus.APPROVED))
                .thenReturn(List.of(lot));
        when(counterService.getAvailableCount(anyLong())).thenReturn(5);

        List<ParkingLotResponse> result = lotService.getLotsByCity("Pune");

        assertThat(result).hasSize(1);
    }

    @Test
    void approveLot_ShouldSetStatusToApproved() {
        ParkingLot lot = ParkingLot.builder().lotId(1L).status(ParkingLot.LotStatus.PENDING).build();
        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

        lotService.approveLot(1L);

        assertThat(lot.getStatus()).isEqualTo(ParkingLot.LotStatus.APPROVED);
        verify(lotRepository).save(lot);
    }

    @Test
    void rejectLot_ShouldSetStatusToRejected() {
        ParkingLot lot = ParkingLot.builder().lotId(1L).status(ParkingLot.LotStatus.PENDING).build();
        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

        lotService.rejectLot(1L);

        assertThat(lot.getStatus()).isEqualTo(ParkingLot.LotStatus.REJECTED);
        verify(lotRepository).save(lot);
    }

    @Test
    void getApprovedLots_ShouldReturnApprovedOnly() {
        ParkingLot lot = ParkingLot.builder().lotId(1L).status(ParkingLot.LotStatus.APPROVED).build();
        when(lotRepository.findByStatus(ParkingLot.LotStatus.APPROVED)).thenReturn(List.of(lot));
        when(counterService.getAvailableCount(anyLong())).thenReturn(3);

        List<ParkingLotResponse> result = lotService.getApprovedLots();

        assertThat(result).hasSize(1);
    }

    @Test
    void getPendingLots_ShouldReturnPendingOnly() {
        ParkingLot lot = ParkingLot.builder().lotId(1L).status(ParkingLot.LotStatus.PENDING).build();
        when(lotRepository.findByStatus(ParkingLot.LotStatus.PENDING)).thenReturn(List.of(lot));

        List<ParkingLotResponse> result = lotService.getPendingLots();

        assertThat(result).hasSize(1);
    }
}
