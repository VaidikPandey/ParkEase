package com.parkease.parking.repository;

import com.parkease.parking.domain.entity.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {
    List<ParkingSpot> findByParkingLot_LotId(Long lotId);
    List<ParkingSpot> findByParkingLot_LotIdAndStatus(Long lotId, ParkingSpot.SpotStatus status);
    List<ParkingSpot> findByParkingLot_LotIdAndSpotType(Long lotId, ParkingSpot.SpotType type);
    List<ParkingSpot> findByParkingLot_LotIdAndIsEv(Long lotId, boolean isEv);
    int countByParkingLot_LotIdAndStatus(Long lotId, ParkingSpot.SpotStatus status);
    int countByParkingLot_LotId(Long lotId);
    boolean existsByParkingLot_LotIdAndSpotNumber(Long lotId, String spotNumber);

    @Modifying
    @Query("UPDATE ParkingSpot p SET p.status = :newStatus WHERE p.spotId = :spotId AND p.status = :oldStatus")
    int updateStatusIfMatches(@Param("spotId") Long spotId,
                              @Param("oldStatus") ParkingSpot.SpotStatus oldStatus,
                              @Param("newStatus") ParkingSpot.SpotStatus newStatus);

    @Modifying
    @Query("UPDATE ParkingSpot p SET p.status = :newStatus WHERE p.spotId = :spotId AND p.status IN :oldStatuses")
    int updateStatusIfMatchesAny(@Param("spotId") Long spotId,
                                 @Param("oldStatuses") List<ParkingSpot.SpotStatus> oldStatuses,
                                 @Param("newStatus") ParkingSpot.SpotStatus newStatus);
}
