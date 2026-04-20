package com.parkease.parking.web.dto.request;

import com.parkease.parking.domain.entity.ParkingSpot;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateSpotRequest {

    @NotBlank(message = "Spot number is required")
    private String spotNumber;

    @NotNull(message = "Floor is required")
    @Min(value = 0, message = "Floor must be 0 or above")
    private Integer floor;

    @NotNull(message = "Spot type is required")
    private ParkingSpot.SpotType spotType;

    @NotNull(message = "Price per hour is required")
    @Positive(message = "Price must be positive")
    private Double pricePerHour;

    private boolean isEv = false;
    private boolean isHandicapped = false;
}
