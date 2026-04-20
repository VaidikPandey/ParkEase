package com.parkease.parking.web.dto.request;

import com.parkease.parking.domain.entity.ParkingSpot;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateSpotRequest {
    private ParkingSpot.SpotType spotType;

    @Positive(message = "Price must be positive")
    private Double pricePerHour;

    private Boolean isEv;
    private Boolean isHandicapped;
    private Boolean isActive;
}
