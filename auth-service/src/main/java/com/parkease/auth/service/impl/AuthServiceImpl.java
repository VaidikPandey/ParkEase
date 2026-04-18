package com.parkease.auth.service.impl;

import com.parkease.auth.domain.entity.User;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.security.JwtUtil;
import com.parkease.auth.service.AuthService;
import com.parkease.auth.web.dto.request.*;
import com.parkease.auth.web.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.expiry-ms}")
    private long expiryMs;

    // ── Register ─────────────────────────────────────────────────────────────

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already registered: " + request.getEmail());
        }
        if (request.getRole() == User.Role.ADMIN) {
            throw new IllegalArgumentException("Admin accounts cannot be self-registered");
        }

        User user = User.builder()
            .fullName(request.getFullName())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .phone(request.getPhone())
            .role(request.getRole())
            .isActive(true)
            .build();

        userRepository.save(user);
        log.info("New user registered: {} [{}]", user.getEmail(), user.getRole());

        return buildAuthResponse(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        return buildAuthResponse(user);
    }

    // ── Logout (blacklist token in Redis) ─────────────────────────────────────

    @Override
    public void logout(String token) {
        if (jwtUtil.isTokenValid(token)) {
            long ttl = jwtUtil.extractClaims(token).getExpiration().getTime()
                       - System.currentTimeMillis();
            if (ttl > 0) {
                redisTemplate.opsForValue()
                    .set("blacklist:" + token, "1", ttl, TimeUnit.MILLISECONDS);
            }
        }
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    @Override
    public AuthResponse refresh(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String email = jwtUtil.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return buildAuthResponse(user);
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return UserProfileResponse.from(findUserById(userId));
    }

    @Override
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getFullName() != null)    user.setFullName(request.getFullName());
        if (request.getPhone() != null)        user.setPhone(request.getPhone());
        if (request.getProfilePicUrl() != null) user.setProfilePicUrl(request.getProfilePicUrl());
        if (request.getVehiclePlate() != null)  user.setVehiclePlate(request.getVehiclePlate());

        return UserProfileResponse.from(userRepository.save(user));
    }

    // ── Change Password ───────────────────────────────────────────────────────

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for userId={}", userId);
    }

    // ── Deactivate ────────────────────────────────────────────────────────────

    @Override
    public void deactivateAccount(Long userId) {
        User user = findUserById(userId);
        user.setActive(false);
        userRepository.save(user);
        log.info("Account deactivated: userId={}", userId);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: id=" + userId));
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtUtil.generateAccessToken(
            user.getEmail(), user.getRole().name(), user.getUserId()
        );
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(expiryMs / 1000)
            .user(UserProfileResponse.from(user))
            .build();
    }
}
