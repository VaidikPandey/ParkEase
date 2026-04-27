package com.parkease.auth.service;

import com.parkease.auth.web.dto.request.*;
import com.parkease.auth.web.dto.response.*;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String token);
    AuthResponse refresh(String refreshToken);
    UserProfileResponse getProfile(Long userId);
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
    void deactivateAccount(Long userId);
    AuthResponse selectRole(Long userId, String role);
}
