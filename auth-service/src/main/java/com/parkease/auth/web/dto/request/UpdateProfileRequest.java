package com.parkease.auth.web.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String phone;
    private String profilePicUrl;
    private String vehiclePlate;
}
