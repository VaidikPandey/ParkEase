package com.parkease.auth.service.impl;

import com.parkease.auth.domain.entity.User;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.security.JwtUtil;
import com.parkease.auth.web.dto.request.RegisterRequest;
import com.parkease.auth.web.dto.request.LoginRequest;
import com.parkease.auth.web.dto.request.ChangePasswordRequest;
import com.parkease.auth.web.dto.response.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "expiryMs", 3600000L);
    }

    @Test
    void register_ShouldCreateUser_WhenEmailIsUnique() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");
        request.setFullName("Test User");
        request.setRole(User.Role.DRIVER);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(jwtUtil.generateAccessToken(anyString(), anyString(), any())).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(anyString())).thenReturn("refresh_token");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        User user = User.builder()
                .userId(1L)
                .email("test@example.com")
                .isActive(true)
                .role(User.Role.DRIVER)
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(user));
        when(jwtUtil.generateAccessToken(anyString(), anyString(), any())).thenReturn("access_token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_ShouldThrowException_WhenUserIsDeactivated() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");

        User user = User.builder().isActive(false).build();
        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class)
                .hasMessageContaining("Account is deactivated");
    }

    @Test
    void changePassword_ShouldUpdatePassword_WhenCurrentPasswordMatches() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old_pass");
        request.setNewPassword("new_pass");
        request.setConfirmPassword("new_pass");

        User user = User.builder().passwordHash("hashed_old").build();
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("old_pass", "hashed_old")).thenReturn(true);
        when(passwordEncoder.encode("new_pass")).thenReturn("hashed_new");

        // Act
        authService.changePassword(1L, request);

        // Assert
        assertThat(user.getPasswordHash()).isEqualTo("hashed_new");
        verify(userRepository).save(user);
    }

    @Test
    void getProfile_ShouldReturnUserProfile() {
        User user = User.builder()
                .userId(1L)
                .email("test@example.com")
                .fullName("Test User")
                .role(User.Role.DRIVER)
                .isActive(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

        var profile = authService.getProfile(1L);

        assertThat(profile).isNotNull();
        assertThat(profile.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void updateProfile_ShouldUpdateFieldsAndSave() {
        User user = User.builder().userId(1L).fullName("Old Name").role(User.Role.DRIVER).build();
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        com.parkease.auth.web.dto.request.UpdateProfileRequest request =
                new com.parkease.auth.web.dto.request.UpdateProfileRequest();
        request.setFullName("New Name");

        var profile = authService.updateProfile(1L, request);

        assertThat(profile).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_ShouldThrow_WhenCurrentPasswordDoesNotMatch() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong_pass");
        request.setNewPassword("new_pass");
        request.setConfirmPassword("new_pass");

        User user = User.builder().passwordHash("hashed_old").build();
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong_pass", "hashed_old")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(1L, request))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }
}
