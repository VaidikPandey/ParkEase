package com.parkease.parking.job;

import com.parkease.parking.domain.entity.ParkingLot;
import com.parkease.parking.domain.entity.ParkingSpot;
import com.parkease.parking.repository.ParkingLotRepository;
import com.parkease.parking.repository.ParkingSpotRepository;
import com.parkease.parking.service.AvailabilityCounterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CounterReconciliationJobTest {

    @Mock private ParkingLotRepository lotRepository;
    @Mock private ParkingSpotRepository spotRepository;
    @Mock private AvailabilityCounterService counterService;

    @InjectMocks
    private CounterReconciliationJob job;

    @Test
    void reconcile_ShouldInitCounterForEachLot() {
        ParkingLot lot1 = ParkingLot.builder().lotId(1L).build();
        ParkingLot lot2 = ParkingLot.builder().lotId(2L).build();

        when(lotRepository.findAll()).thenReturn(List.of(lot1, lot2));
        when(spotRepository.countByParkingLot_LotIdAndStatus(eq(1L), eq(ParkingSpot.SpotStatus.AVAILABLE))).thenReturn(5);
        when(spotRepository.countByParkingLot_LotIdAndStatus(eq(2L), eq(ParkingSpot.SpotStatus.AVAILABLE))).thenReturn(3);

        job.reconcile();

        verify(counterService).initCounter(1L, 5);
        verify(counterService).initCounter(2L, 3);
    }

    @Test
    void reconcile_ShouldDoNothing_WhenNoLots() {
        when(lotRepository.findAll()).thenReturn(List.of());

        job.reconcile();

        verifyNoInteractions(counterService);
    }
}
