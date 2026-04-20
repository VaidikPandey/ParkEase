package com.parkease.auth.service.impl;

import com.parkease.auth.domain.entity.User;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.service.AdminService;
import com.parkease.auth.web.dto.response.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;

    // ── Get All Users

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserProfileResponse::from)
                .toList();
    }

    // ── Get Users By Role

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getUsersByRole(User.Role role) {
        return userRepository.findAllByRole(role)
                .stream()
                .map(UserProfileResponse::from)
                .toList();
    }

    // ── Get User By ID

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Long userId) {
        return UserProfileResponse.from(findUserById(userId));
    }

    // ── Suspend User

    @Override
    public void suspendUser(Long userId) {
        User user = findUserById(userId);

        if (!user.isActive()) {
            throw new IllegalStateException("User is already suspended: id=" + userId);
        }
        if (user.getRole() == User.Role.ADMIN) {
            throw new IllegalArgumentException("Cannot suspend an admin account");
        }

        user.setActive(false);
        userRepository.save(user);
        log.info("Admin suspended userId={}", userId);
    }

    // ── Reactivate User

    @Override
    public void reactivateUser(Long userId) {
        User user = findUserById(userId);

        if (user.isActive()) {
            throw new IllegalStateException("User is already active: id=" + userId);
        }

        user.setActive(true);
        userRepository.save(user);
        log.info("Admin reactivated userId={}", userId);
    }

    // ── Delete User

    @Override
    public void deleteUser(Long userId) {
        User user = findUserById(userId);

        if (user.getRole() == User.Role.ADMIN) {
            throw new IllegalArgumentException("Cannot delete an admin account");
        }

        userRepository.deleteById(userId);
        log.info("Admin permanently deleted userId={}", userId);
    }

    // ── Private Helper

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: id=" + userId));
    }
}