package com.parkease.parking.job;

import com.parkease.parking.domain.entity.ParkingSpot;
import com.parkease.parking.repository.ParkingLotRepository;
import com.parkease.parking.repository.ParkingSpotRepository;
import com.parkease.parking.service.AvailabilityCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CounterReconciliationJob {

    private final ParkingLotRepository lotRepository;
    private final ParkingSpotRepository spotRepository;
    private final AvailabilityCounterService counterService;

    @Scheduled(fixedDelay = 300_000)
    public void reconcile() {
        lotRepository.findAll().forEach(lot -> {
            int actual = spotRepository.countByParkingLot_LotIdAndStatus(
                    lot.getLotId(), ParkingSpot.SpotStatus.AVAILABLE);
            counterService.initCounter(lot.getLotId(), actual);
            log.debug("Reconciled lot {} → {} available spots", lot.getLotId(), actual);
        });
        log.info("Counter reconciliation complete");
    }
}
