package com.parkease.parking.web.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateLotRequest {

    @NotBlank(message = "Lot name is required")
    @Size(min = 3, max = 100)
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    @NotBlank(message = "Opening time is required")
    private String openingTime;

    @NotBlank(message = "Closing time is required")
    private String closingTime;

    private String imageUrl;
}
