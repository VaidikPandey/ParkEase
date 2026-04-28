package com.parkease.parking.repository;

import com.parkease.parking.domain.entity.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {
    List<ParkingSpot> findByParkingLot_LotId(Long lotId);
    List<ParkingSpot> findByParkingLot_LotIdAndStatus(Long lotId, ParkingSpot.SpotStatus status);
    List<ParkingSpot> findByParkingLot_LotIdAndSpotType(Long lotId, ParkingSpot.SpotType type);
    List<ParkingSpot> findByParkingLot_LotIdAndIsEv(Long lotId, boolean isEv);
    int countByParkingLot_LotIdAndStatus(Long lotId, ParkingSpot.SpotStatus status);
    int countByParkingLot_LotId(Long lotId);
    boolean existsByParkingLot_LotIdAndSpotNumber(Long lotId, String spotNumber);
}
