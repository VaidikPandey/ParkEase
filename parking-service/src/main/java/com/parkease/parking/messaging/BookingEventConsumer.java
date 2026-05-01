package com.parkease.parking.messaging;

import com.parkease.parking.config.RabbitMQConfig;
import com.parkease.parking.domain.entity.ParkingLot;
import com.parkease.parking.domain.entity.ParkingSpot;
import com.parkease.parking.repository.ParkingLotRepository;
import com.parkease.parking.repository.ParkingSpotRepository;
import com.parkease.parking.service.AvailabilityCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Lazy(false)
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final ParkingSpotRepository spotRepository;
    private final ParkingLotRepository  lotRepository;
    private final AvailabilityCounterService counterService;
    private final RabbitTemplate rabbitTemplate;

    private static final List<Integer> THRESHOLDS = List.of(50, 80, 100);

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
    @org.springframework.transaction.annotation.Transactional
    void reserveSpot(Map<String, Object> event) {
        Long spotId = toLong(event.get("spotId"));
        Long lotId  = toLong(event.get("lotId"));
        if (spotId == null || lotId == null) return;

        int prevOccupied = countOccupied(lotId);
        int updated = spotRepository.updateStatusIfMatches(
                spotId, ParkingSpot.SpotStatus.AVAILABLE, ParkingSpot.SpotStatus.RESERVED);

        if (updated == 0) {
            log.warn("Spot {} was not AVAILABLE — skipping reserve (duplicate or late event)", spotId);
            return;
        }
        counterService.decrement(lotId);
        log.info("Spot {} → RESERVED (booking confirmed)", spotId);
        checkCapacityThresholds(lotId, prevOccupied, prevOccupied + 1);
    }

    // check-in → spot OCCUPIED (counter already decremented on reserve)
    @org.springframework.transaction.annotation.Transactional
    void occupySpot(Map<String, Object> event) {
        Long spotId = toLong(event.get("spotId"));
        Long lotId  = toLong(event.get("lotId"));
        if (spotId == null) return;

        int prevOccupied = lotId != null ? countOccupied(lotId) : 0;
        int updated = spotRepository.updateStatusIfMatches(
                spotId, ParkingSpot.SpotStatus.RESERVED, ParkingSpot.SpotStatus.OCCUPIED);

        if (updated == 0) {
            log.warn("Spot {} was not RESERVED — skipping occupy (duplicate or late event)", spotId);
            return;
        }
        log.info("Spot {} → OCCUPIED (check-in)", spotId);
        if (lotId != null) checkCapacityThresholds(lotId, prevOccupied, prevOccupied + 1);
    }

    // checkout / cancel / expiry → spot AVAILABLE, increment counter
    @org.springframework.transaction.annotation.Transactional
    void releaseSpot(Map<String, Object> event) {
        Long spotId = toLong(event.get("spotId"));
        Long lotId  = toLong(event.get("lotId"));
        if (spotId == null || lotId == null) return;

        int prevOccupied = countOccupied(lotId);
        int updated = spotRepository.updateStatusIfMatchesAny(
                spotId,
                List.of(ParkingSpot.SpotStatus.RESERVED, ParkingSpot.SpotStatus.OCCUPIED),
                ParkingSpot.SpotStatus.AVAILABLE);

        if (updated == 0) {
            log.warn("Spot {} was already AVAILABLE — skipping release (duplicate or late event)", spotId);
            return;
        }
        counterService.increment(lotId);
        log.info("Spot {} → AVAILABLE ({})", spotId, event.get("eventType"));
        checkCapacityThresholds(lotId, prevOccupied, prevOccupied - 1);
    }

    private void checkCapacityThresholds(Long lotId, int prevOccupied, int currOccupied) {
        int total = spotRepository.countByParkingLot_LotId(lotId);
        if (total == 0) return;

        int prevPercent = (prevOccupied * 100) / total;
        int currPercent = (currOccupied * 100) / total;

        for (int threshold : THRESHOLDS) {
            if (prevPercent < threshold && currPercent >= threshold) {
                publishCapacityEvent(lotId, threshold, currPercent, currOccupied, total);
            }
        }
    }

    private void publishCapacityEvent(Long lotId, int threshold, int occupancyPercent,
                                       int occupiedSpots, int totalSpots) {
        Optional<ParkingLot> lotOpt = lotRepository.findById(lotId);
        if (lotOpt.isEmpty()) return;
        ParkingLot lot = lotOpt.get();

        Map<String, Object> event = new HashMap<>();
        event.put("eventType",       "lot.capacity.threshold");
        event.put("lotId",           lotId);
        event.put("lotName",         lot.getName());
        event.put("managerId",       lot.getManagerId());
        event.put("threshold",       threshold);
        event.put("occupancyPercent", occupancyPercent);
        event.put("occupiedSpots",   occupiedSpots);
        event.put("totalSpots",      totalSpots);

        rabbitTemplate.convertAndSend(RabbitMQConfig.BOOKING_EXCHANGE, "lot.capacity", event);
        log.info("Capacity threshold {} crossed for lot {} ({}/{})", threshold, lot.getName(), occupiedSpots, totalSpots);
    }

    private int countOccupied(Long lotId) {
        return spotRepository.countByParkingLot_LotIdAndStatus(lotId, ParkingSpot.SpotStatus.RESERVED)
             + spotRepository.countByParkingLot_LotIdAndStatus(lotId, ParkingSpot.SpotStatus.OCCUPIED);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }
}
