package com.parkease.auth.service;

import com.parkease.auth.domain.entity.User;
import com.parkease.auth.web.dto.response.UserProfileResponse;

import java.util.List;

public interface AdminService {
    List<UserProfileResponse> getAllUsers();
    List<UserProfileResponse> getUsersByRole(User.Role role);
    UserProfileResponse getUserById(Long userId);
    void suspendUser(Long userId);
    void reactivateUser(Long userId);
    void deleteUser(Long userId);
}