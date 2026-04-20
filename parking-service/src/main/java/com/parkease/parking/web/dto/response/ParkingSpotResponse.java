package com.parkease.parking.web.dto.response;

import com.parkease.parking.domain.entity.ParkingSpot;
import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ParkingSpotResponse {

    private Long spotId;
    private Long lotId;
    private String spotNumber;
    private int floor;
    private String spotType;
    private String status;
    private Double pricePerHour;
    private boolean isEv;
    private boolean isHandicapped;
    private boolean isActive;
    private LocalDateTime createdAt;

    public static ParkingSpotResponse from(ParkingSpot spot) {
        return ParkingSpotResponse.builder()
            .spotId(spot.getSpotId())
            .lotId(spot.getParkingLot().getLotId())
            .spotNumber(spot.getSpotNumber())
            .floor(spot.getFloor())
            .spotType(spot.getSpotType().name())
            .status(spot.getStatus().name())
            .pricePerHour(spot.getPricePerHour())
            .isEv(spot.isEv())
            .isHandicapped(spot.isHandicapped())
            .isActive(spot.isActive())
            .createdAt(spot.getCreatedAt())
            .build();
    }
}
