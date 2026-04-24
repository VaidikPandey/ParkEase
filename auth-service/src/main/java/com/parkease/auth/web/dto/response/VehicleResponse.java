package com.parkease.auth.web.dto.response;

import com.parkease.auth.domain.entity.Vehicle;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VehicleResponse {

    private Long id;
    private Long userId;
    private String plate;
    private String vehicleType;
    private boolean isEv;
    private String nickname;
    private LocalDateTime createdAt;

    public static VehicleResponse from(Vehicle v) {
        return VehicleResponse.builder()
                .id(v.getId())
                .userId(v.getUserId())
                .plate(v.getPlate().toUpperCase())
                .vehicleType(v.getVehicleType().name())
                .isEv(v.isEv())
                .nickname(v.getNickname())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
