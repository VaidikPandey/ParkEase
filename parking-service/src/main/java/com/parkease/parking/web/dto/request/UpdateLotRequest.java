package com.parkease.parking.web.dto.request;

import lombok.Data;

@Data
public class UpdateLotRequest {
    private String name;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private String openingTime;
    private String closingTime;
    private String imageUrl;
}
