package com.parkease.parking.messaging;

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
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingEventConsumerTest {

    @Mock private ParkingSpotRepository spotRepository;
    @Mock private ParkingLotRepository lotRepository;
    @Mock private AvailabilityCounterService counterService;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private BookingEventConsumer consumer;

    private Map<String, Object> event(String type, long spotId, long lotId) {
        Map<String, Object> e = new HashMap<>();
        e.put("eventType", type);
        e.put("spotId", spotId);
        e.put("lotId", lotId);
        return e;
    }

    @Test
    void handle_ShouldReserveSpot_WhenBookingConfirmed() {
        when(spotRepository.countByParkingLot_LotIdAndStatus(anyLong(), any())).thenReturn(0);
        when(spotRepository.updateStatusIfMatches(anyLong(), any(), any())).thenReturn(1);
        when(spotRepository.countByParkingLot_LotId(anyLong())).thenReturn(10);

        consumer.handle(event("booking.confirmed", 1L, 1L));

        verify(counterService).decrement(1L);
    }

    @Test
    void handle_ShouldOccupySpot_WhenCheckin() {
        when(spotRepository.countByParkingLot_LotIdAndStatus(anyLong(), any())).thenReturn(0);
        when(spotRepository.updateStatusIfMatches(anyLong(), any(), any())).thenReturn(1);
        when(spotRepository.countByParkingLot_LotId(anyLong())).thenReturn(10);

        consumer.handle(event("booking.checkin", 1L, 1L));

        verify(spotRepository).updateStatusIfMatches(1L, ParkingSpot.SpotStatus.RESERVED, ParkingSpot.SpotStatus.OCCUPIED);
    }

    @Test
    void handle_ShouldReleaseSpot_WhenCheckout() {
        when(spotRepository.countByParkingLot_LotIdAndStatus(anyLong(), any())).thenReturn(0);
        when(spotRepository.updateStatusIfMatchesAny(anyLong(), anyList(), any())).thenReturn(1);
        when(spotRepository.countByParkingLot_LotId(anyLong())).thenReturn(10);

        consumer.handle(event("booking.checkout", 1L, 1L));

        verify(counterService).increment(1L);
    }

    @Test
    void handle_ShouldReleaseSpot_WhenCancelled() {
        when(spotRepository.countByParkingLot_LotIdAndStatus(anyLong(), any())).thenReturn(0);
        when(spotRepository.updateStatusIfMatchesAny(anyLong(), anyList(), any())).thenReturn(1);
        when(spotRepository.countByParkingLot_LotId(anyLong())).thenReturn(10);

        consumer.handle(event("booking.cancelled", 1L, 1L));

        verify(counterService).increment(1L);
    }

    @Test
    void handle_ShouldReleaseSpot_WhenExpiry() {
        when(spotRepository.countByParkingLot_LotIdAndStatus(anyLong(), any())).thenReturn(0);
        when(spotRepository.updateStatusIfMatchesAny(anyLong(), anyList(), any())).thenReturn(1);
        when(spotRepository.countByParkingLot_LotId(anyLong())).thenReturn(10);

        consumer.handle(event("booking.expiry", 1L, 1L));

        verify(counterService).increment(1L);
    }

    @Test
    void handle_ShouldSkipUnknownEventType() {
        consumer.handle(event("unknown.event", 1L, 1L));

        verifyNoInteractions(counterService);
    }

    @Test
    void handle_ShouldSkipNullEventType() {
        Map<String, Object> e = new HashMap<>();
        e.put("spotId", 1L);

        consumer.handle(e);

        verifyNoInteractions(counterService);
    }

    @Test
    void reserveSpot_ShouldSkip_WhenSpotNotAvailable() {
        when(spotRepository.countByParkingLot_LotIdAndStatus(anyLong(), any())).thenReturn(0);
        when(spotRepository.updateStatusIfMatches(anyLong(), any(), any())).thenReturn(0);

        consumer.reserveSpot(event("booking.confirmed", 1L, 1L));

        verify(counterService, never()).decrement(any());
    }

    @Test
    void releaseSpot_ShouldSkip_WhenSpotAlreadyAvailable() {
        when(spotRepository.countByParkingLot_LotIdAndStatus(anyLong(), any())).thenReturn(0);
        when(spotRepository.updateStatusIfMatchesAny(anyLong(), anyList(), any())).thenReturn(0);

        consumer.releaseSpot(event("booking.checkout", 1L, 1L));

        verify(counterService, never()).increment(any());
    }

    @Test
    void handle_ShouldPublishCapacityEvent_WhenThresholdCrossed() {
        ParkingLot lot = ParkingLot.builder().lotId(1L).name("Test Lot").managerId(10L).build();
        when(spotRepository.countByParkingLot_LotIdAndStatus(eq(1L), eq(ParkingSpot.SpotStatus.RESERVED))).thenReturn(4);
        when(spotRepository.countByParkingLot_LotIdAndStatus(eq(1L), eq(ParkingSpot.SpotStatus.OCCUPIED))).thenReturn(0);
        when(spotRepository.updateStatusIfMatches(anyLong(), any(), any())).thenReturn(1);
        when(spotRepository.countByParkingLot_LotId(1L)).thenReturn(10);
        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

        consumer.reserveSpot(event("booking.confirmed", 1L, 1L));

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Map.class));
    }
}
