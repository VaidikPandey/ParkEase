package com.parkease.parking.web.dto.response;

import com.parkease.parking.domain.entity.ParkingLot;
import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ParkingLotResponse {

    private Long lotId;
    private Long managerId;
    private String name;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private String openingTime;
    private String closingTime;
    private String imageUrl;
    private String status;
    private int totalSpots;
    private int maxCapacity;
    private int availableSpots;
    private LocalDateTime createdAt;

    public static ParkingLotResponse from(ParkingLot lot, int availableSpots) {
        return ParkingLotResponse.builder()
            .lotId(lot.getLotId())
            .managerId(lot.getManagerId())
            .name(lot.getName())
            .address(lot.getAddress())
            .city(lot.getCity())
            .latitude(lot.getLatitude())
            .longitude(lot.getLongitude())
            .openingTime(lot.getOpeningTime())
            .closingTime(lot.getClosingTime())
            .imageUrl(lot.getImageUrl())
            .status(lot.getStatus().name())
            .totalSpots(lot.getTotalSpots())
            .maxCapacity(lot.getMaxCapacity())
            .availableSpots(availableSpots)
            .createdAt(lot.getCreatedAt())
            .build();
    }
}
