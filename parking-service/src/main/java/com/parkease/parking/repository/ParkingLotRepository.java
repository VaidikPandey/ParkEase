package com.parkease.parking.repository;

import com.parkease.parking.domain.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {
    List<ParkingLot> findByCity(String city);
    List<ParkingLot> findByManagerId(Long managerId);
    List<ParkingLot> findByStatus(ParkingLot.LotStatus status);
    List<ParkingLot> findByCityAndStatus(String city, ParkingLot.LotStatus status);

    @Query(value = """
        SELECT * FROM parking_lots
        WHERE status = 'APPROVED'
        AND (6371 * acos(
            cos(radians(:lat)) * cos(radians(latitude))
            * cos(radians(longitude) - radians(:lng))
            + sin(radians(:lat)) * sin(radians(latitude))
        )) <= :radius
        ORDER BY (6371 * acos(
            cos(radians(:lat)) * cos(radians(latitude))
            * cos(radians(longitude) - radians(:lng))
            + sin(radians(:lat)) * sin(radians(latitude))
        ))
        """, nativeQuery = true)
    List<ParkingLot> findNearbyLots(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radius") double radiusKm
    );
}
