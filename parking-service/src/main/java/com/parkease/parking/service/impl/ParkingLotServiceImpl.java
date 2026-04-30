package com.parkease.parking.service.impl;

import com.parkease.parking.domain.entity.ParkingLot;
import com.parkease.parking.domain.entity.ParkingSpot;
import com.parkease.parking.repository.ParkingLotRepository;
import com.parkease.parking.repository.ParkingSpotRepository;
import com.parkease.parking.service.AvailabilityCounterService;
import com.parkease.parking.service.ParkingLotService;
import com.parkease.parking.web.dto.request.CreateLotRequest;
import com.parkease.parking.web.dto.request.UpdateLotRequest;
import com.parkease.parking.web.dto.response.ParkingLotResponse;
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
public class ParkingLotServiceImpl implements ParkingLotService {

    private final ParkingLotRepository lotRepository;
    private final ParkingSpotRepository spotRepository;
    private final AvailabilityCounterService counterService;

    @Override
    public ParkingLotResponse createLot(Long managerId, CreateLotRequest request) {
        ParkingLot lot = ParkingLot.builder()
                .managerId(managerId)
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .imageUrl(request.getImageUrl())
                .status(ParkingLot.LotStatus.PENDING)
                .totalSpots(0)
                .maxCapacity(request.getMaxCapacity())
                .build();

        lotRepository.save(lot);

        // init Redis counter at 0 — lot has no spots yet
        counterService.initCounter(lot.getLotId(), 0);

        log.info("New lot created by managerId={}: {}", managerId, lot.getName());
        return ParkingLotResponse.from(lot, 0);
    }

    @Override
    public ParkingLotResponse updateLot(Long lotId, Long managerId, UpdateLotRequest request) {
        ParkingLot lot = findLotById(lotId);
        validateManagerOwnership(lot, managerId);

        if (request.getName() != null)        lot.setName(request.getName());
        if (request.getAddress() != null)     lot.setAddress(request.getAddress());
        if (request.getCity() != null)        lot.setCity(request.getCity());
        if (request.getLatitude() != null)    lot.setLatitude(request.getLatitude());
        if (request.getLongitude() != null)   lot.setLongitude(request.getLongitude());
        if (request.getOpeningTime() != null) lot.setOpeningTime(request.getOpeningTime());
        if (request.getClosingTime() != null) lot.setClosingTime(request.getClosingTime());
        if (request.getImageUrl() != null)    lot.setImageUrl(request.getImageUrl());

        int available = counterService.getAvailableCount(lotId);
        return ParkingLotResponse.from(lotRepository.save(lot), available);
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingLotResponse getLotById(Long lotId) {
        ParkingLot lot = findLotById(lotId);
        // read from Redis instead of DB
        int available = counterService.getAvailableCount(lotId);
        return ParkingLotResponse.from(lot, available);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLotResponse> getLotsByManager(Long managerId) {
        return lotRepository.findByManagerId(managerId)
                .stream()
                .map(lot -> {
                    int available = counterService.getAvailableCount(lot.getLotId());
                    return ParkingLotResponse.from(lot, available);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLotResponse> getLotsByCity(String city) {
        return lotRepository.findByCityAndStatus(city, ParkingLot.LotStatus.APPROVED)
                .stream()
                .map(lot -> {
                    int available = counterService.getAvailableCount(lot.getLotId());
                    return ParkingLotResponse.from(lot, available);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLotResponse> getNearbyLots(double lat, double lng, double radiusKm) {
        return lotRepository.findNearbyLots(lat, lng, radiusKm)
                .stream()
                .map(lot -> {
                    int available = counterService.getAvailableCount(lot.getLotId());
                    return ParkingLotResponse.from(lot, available);
                })
                .toList();
    }

    @Override
    public ParkingLotResponse toggleLotStatus(Long lotId, Long managerId) {
        ParkingLot lot = findLotById(lotId);
        validateManagerOwnership(lot, managerId);

        if (lot.getStatus() == ParkingLot.LotStatus.APPROVED) {
            lot.setStatus(ParkingLot.LotStatus.CLOSED);
        } else if (lot.getStatus() == ParkingLot.LotStatus.CLOSED) {
            lot.setStatus(ParkingLot.LotStatus.APPROVED);
        } else {
            throw new IllegalStateException("Cannot toggle a lot that is PENDING or REJECTED");
        }

        lotRepository.save(lot);
        log.info("Lot {} status toggled to {}", lotId, lot.getStatus());
        int available = counterService.getAvailableCount(lotId);
        return ParkingLotResponse.from(lot, available);
    }

    @Override
    public void approveLot(Long lotId) {
        ParkingLot lot = findLotById(lotId);
        if (lot.getStatus() != ParkingLot.LotStatus.PENDING) {
            throw new IllegalStateException("Only PENDING lots can be approved");
        }
        lot.setStatus(ParkingLot.LotStatus.APPROVED);
        lotRepository.save(lot);

        // sync Redis counter with actual DB count on approval
        int available = spotRepository.countByParkingLot_LotIdAndStatus(
                lotId, ParkingSpot.SpotStatus.AVAILABLE
        );
        counterService.initCounter(lotId, available);

        log.info("Lot {} approved, Redis counter synced to {}", lotId, available);
    }

    @Override
    public void rejectLot(Long lotId) {
        ParkingLot lot = findLotById(lotId);
        if (lot.getStatus() != ParkingLot.LotStatus.PENDING) {
            throw new IllegalStateException("Only PENDING lots can be rejected");
        }
        lot.setStatus(ParkingLot.LotStatus.REJECTED);
        lotRepository.save(lot);

        // clean up Redis counter
        counterService.deleteCounter(lotId);

        log.info("Lot {} rejected, Redis counter deleted", lotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLotResponse> getPendingLots() {
        return lotRepository.findByStatus(ParkingLot.LotStatus.PENDING)
                .stream()
                .map(lot -> ParkingLotResponse.from(lot, 0))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLotResponse> getApprovedLots() {
        return lotRepository.findByStatus(ParkingLot.LotStatus.APPROVED)
                .stream()
                .map(lot -> {
                    int available = counterService.getAvailableCount(lot.getLotId());
                    return ParkingLotResponse.from(lot, available);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLotResponse> getAllLots() {
        return lotRepository.findAll()
                .stream()
                .map(lot -> {
                    int available = counterService.getAvailableCount(lot.getLotId());
                    return ParkingLotResponse.from(lot, available);
                })
                .toList();
    }

    @Override
    public void deleteLotsByManager(Long managerId) {
        List<ParkingLot> lots = lotRepository.findByManagerId(managerId);
        lots.forEach(lot -> counterService.deleteCounter(lot.getLotId()));
        lotRepository.deleteAll(lots);
        log.info("Deleted {} lots for managerId={}", lots.size(), managerId);
    }

    // ── Private Helpers

    private ParkingLot findLotById(Long lotId) {
        return lotRepository.findById(lotId)
                .orElseThrow(() -> new EntityNotFoundException("Lot not found: id=" + lotId));
    }

    private void validateManagerOwnership(ParkingLot lot, Long managerId) {
        if (!lot.getManagerId().equals(managerId)) {
            throw new IllegalArgumentException("You do not own this parking lot");
        }
    }
}