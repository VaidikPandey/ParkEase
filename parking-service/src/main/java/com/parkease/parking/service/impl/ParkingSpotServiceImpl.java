package com.parkease.parking.service.impl;

import com.parkease.parking.domain.entity.ParkingLot;
import com.parkease.parking.domain.entity.ParkingSpot;
import com.parkease.parking.repository.ParkingLotRepository;
import com.parkease.parking.repository.ParkingSpotRepository;
import com.parkease.parking.service.AvailabilityCounterService;
import com.parkease.parking.service.ParkingSpotService;
import com.parkease.parking.web.dto.request.BulkCreateSpotRequest;
import com.parkease.parking.web.dto.request.CreateSpotRequest;
import com.parkease.parking.web.dto.request.UpdateSpotRequest;
import com.parkease.parking.web.dto.response.ParkingSpotResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ParkingSpotServiceImpl implements ParkingSpotService {

    private final ParkingSpotRepository spotRepository;
    private final ParkingLotRepository lotRepository;
    private final AvailabilityCounterService counterService;

    @Override
    public ParkingSpotResponse addSpot(Long lotId, Long managerId, CreateSpotRequest request) {
        ParkingLot lot = findAndValidateLot(lotId, managerId);

        if (spotRepository.existsByParkingLot_LotIdAndSpotNumber(lotId, request.getSpotNumber())) {
            throw new IllegalStateException(
                    "Spot number already exists in this lot: " + request.getSpotNumber()
            );
        }

        ParkingSpot spot = buildSpot(lot, request);
        spotRepository.save(spot);

        lot.setTotalSpots(lot.getTotalSpots() + 1);
        lotRepository.save(lot);

        // increment Redis counter — new spot is AVAILABLE
        counterService.increment(lotId);

        log.info("Spot {} added to lot {}", spot.getSpotNumber(), lotId);
        return ParkingSpotResponse.from(spot);
    }

    @Override
    public List<ParkingSpotResponse> bulkAddSpots(
            Long lotId, Long managerId, BulkCreateSpotRequest request
    ) {
        ParkingLot lot = findAndValidateLot(lotId, managerId);

        List<ParkingSpot> spots = request.getSpots().stream()
                .map(r -> {
                    if (spotRepository.existsByParkingLot_LotIdAndSpotNumber(
                            lotId, r.getSpotNumber())) {
                        throw new IllegalStateException(
                                "Duplicate spot number: " + r.getSpotNumber()
                        );
                    }
                    return buildSpot(lot, r);
                })
                .toList();

        spotRepository.saveAll(spots);

        lot.setTotalSpots(lot.getTotalSpots() + spots.size());
        lotRepository.save(lot);

        // increment Redis counter for each new spot
        spots.forEach(s -> counterService.increment(lotId));

        log.info("{} spots bulk added to lot {}", spots.size(), lotId);
        return spots.stream().map(ParkingSpotResponse::from).toList();
    }

    @Override
    public ParkingSpotResponse updateSpot(
            Long spotId, Long managerId, UpdateSpotRequest request
    ) {
        ParkingSpot spot = findSpotById(spotId);
        validateSpotOwnership(spot, managerId);

        if (request.getSpotType() != null)      spot.setSpotType(request.getSpotType());
        if (request.getPricePerHour() != null)  spot.setPricePerHour(request.getPricePerHour());
        if (request.getIsEv() != null)          spot.setEv(request.getIsEv());
        if (request.getIsHandicapped() != null) spot.setHandicapped(request.getIsHandicapped());
        if (request.getIsActive() != null)      spot.setActive(request.getIsActive());

        return ParkingSpotResponse.from(spotRepository.save(spot));
    }

    @Override
    public void deleteSpot(Long spotId, Long managerId) {
        ParkingSpot spot = findSpotById(spotId);
        validateSpotOwnership(spot, managerId);

        ParkingLot lot = spot.getParkingLot();
        Long lotId = lot.getLotId();

        // only decrement Redis if spot was AVAILABLE
        // if it was RESERVED/OCCUPIED the counter wasn't counting it anyway
        if (spot.getStatus() == ParkingSpot.SpotStatus.AVAILABLE) {
            counterService.decrement(lotId);
        }

        spotRepository.delete(spot);

        lot.setTotalSpots(Math.max(0, lot.getTotalSpots() - 1));
        lotRepository.save(lot);

        log.info("Spot {} deleted from lot {}", spotId, lotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpotResponse> getSpotsByLot(Long lotId) {
        return spotRepository.findByParkingLot_LotId(lotId)
                .stream().map(ParkingSpotResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpotResponse> getAvailableSpots(Long lotId) {
        return spotRepository.findByParkingLot_LotIdAndStatus(
                        lotId, ParkingSpot.SpotStatus.AVAILABLE)
                .stream().map(ParkingSpotResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpotResponse> getSpotsByType(Long lotId, ParkingSpot.SpotType type) {
        return spotRepository.findByParkingLot_LotIdAndSpotType(lotId, type)
                .stream().map(ParkingSpotResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingSpotResponse getSpotById(Long spotId) {
        return ParkingSpotResponse.from(findSpotById(spotId));
    }

    // ── Private Helpers

    private ParkingLot findAndValidateLot(Long lotId, Long managerId) {
        ParkingLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new EntityNotFoundException("Lot not found: id=" + lotId));
        if (!lot.getManagerId().equals(managerId)) {
            throw new IllegalArgumentException("You do not own this parking lot");
        }
        return lot;
    }

    private ParkingSpot findSpotById(Long spotId) {
        return spotRepository.findById(spotId)
                .orElseThrow(() -> new EntityNotFoundException("Spot not found: id=" + spotId));
    }

    private void validateSpotOwnership(ParkingSpot spot, Long managerId) {
        if (!spot.getParkingLot().getManagerId().equals(managerId)) {
            throw new IllegalArgumentException("You do not own this spot");
        }
    }

    private ParkingSpot buildSpot(ParkingLot lot, CreateSpotRequest r) {
        return ParkingSpot.builder()
                .parkingLot(lot)
                .spotNumber(r.getSpotNumber())
                .floor(r.getFloor())
                .spotType(r.getSpotType())
                .status(ParkingSpot.SpotStatus.AVAILABLE)
                .pricePerHour(r.getPricePerHour())
                .isEv(r.isEv())
                .isHandicapped(r.isHandicapped())
                .isActive(true)
                .build();
    }
}