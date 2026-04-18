package com.parkease.auth.web.dto.response;

import com.parkease.auth.domain.entity.User;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {

    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String profilePicUrl;
    private String vehiclePlate;
    private boolean isActive;
    private LocalDateTime createdAt;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
            .userId(user.getUserId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .role(user.getRole().name())
            .profilePicUrl(user.getProfilePicUrl())
            .vehiclePlate(user.getVehiclePlate())
            .isActive(user.isActive())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
