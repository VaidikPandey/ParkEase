package com.parkease.parking.messaging;

import com.parkease.parking.config.RabbitMQConfig;
import com.parkease.parking.domain.entity.ParkingSpot;
import com.parkease.parking.repository.ParkingSpotRepository;
import com.parkease.parking.service.AvailabilityCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Lazy(false)
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final ParkingSpotRepository spotRepository;
    private final AvailabilityCounterService counterService;

    @RabbitListener(queues = RabbitMQConfig.PARKING_QUEUE)
    public void handle(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            if (eventType == null) return;

            switch (eventType) {
                case "booking.confirmed" -> reserveSpot(event);
                case "booking.checkin"   -> occupySpot(event);
                case "booking.checkout",
                     "booking.cancelled",
                     "booking.expiry"    -> releaseSpot(event);
                default -> log.debug("Skipping event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing booking event in parking-service: {}", e.getMessage(), e);
        }
    }

    // booking confirmed → spot RESERVED, decrement counter
    private void reserveSpot(Map<String, Object> event) {
        Long spotId = toLong(event.get("spotId"));
        Long lotId  = toLong(event.get("lotId"));
        if (spotId == null || lotId == null) return;

        spotRepository.findById(spotId).ifPresent(spot -> {
            if (spot.getStatus() == ParkingSpot.SpotStatus.AVAILABLE) {
                spot.setStatus(ParkingSpot.SpotStatus.RESERVED);
                spotRepository.save(spot);
                counterService.decrement(lotId);
                log.info("Spot {} → RESERVED (booking confirmed)", spotId);
            }
        });
    }

    // check-in → spot OCCUPIED (already removed from counter on reserve)
    private void occupySpot(Map<String, Object> event) {
        Long spotId = toLong(event.get("spotId"));
        if (spotId == null) return;

        spotRepository.findById(spotId).ifPresent(spot -> {
            if (spot.getStatus() == ParkingSpot.SpotStatus.RESERVED) {
                spot.setStatus(ParkingSpot.SpotStatus.OCCUPIED);
                spotRepository.save(spot);
                log.info("Spot {} → OCCUPIED (check-in)", spotId);
            }
        });
    }

    // checkout / cancel / expiry → spot AVAILABLE, increment counter
    private void releaseSpot(Map<String, Object> event) {
        Long spotId = toLong(event.get("spotId"));
        Long lotId  = toLong(event.get("lotId"));
        if (spotId == null || lotId == null) return;

        spotRepository.findById(spotId).ifPresent(spot -> {
            ParkingSpot.SpotStatus prev = spot.getStatus();
            if (prev == ParkingSpot.SpotStatus.RESERVED || prev == ParkingSpot.SpotStatus.OCCUPIED) {
                spot.setStatus(ParkingSpot.SpotStatus.AVAILABLE);
                spotRepository.save(spot);
                counterService.increment(lotId);
                log.info("Spot {} → AVAILABLE ({})", spotId, event.get("eventType"));
            }
        });
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }
}
