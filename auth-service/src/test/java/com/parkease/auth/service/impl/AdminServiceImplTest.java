package com.parkease.auth.service.impl;

import com.parkease.auth.domain.entity.User;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.web.dto.response.UserProfileResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void getAllUsers_ShouldReturnList() {
        User user = User.builder().userId(1L).email("test@test.com").role(User.Role.DRIVER).build();
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserProfileResponse> result = adminService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void getUsersByRole_ShouldReturnFilteredList() {
        User user = User.builder().userId(1L).email("mgr@test.com").role(User.Role.MANAGER).build();
        when(userRepository.findAllByRole(User.Role.MANAGER)).thenReturn(List.of(user));

        List<UserProfileResponse> result = adminService.getUsersByRole(User.Role.MANAGER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("mgr@test.com");
    }

    @Test
    void getUserById_ShouldReturnUser_WhenFound() {
        User user = User.builder().userId(1L).email("u@test.com").role(User.Role.DRIVER).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse result = adminService.getUserById(1L);

        assertThat(result.getEmail()).isEqualTo("u@test.com");
    }

    @Test
    void getUserById_ShouldThrow_WhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUserById(99L))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void suspendUser_ShouldDeactivateUser_WhenValid() {
        User user = User.builder().userId(1L).isActive(true).role(User.Role.DRIVER).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminService.suspendUser(1L);

        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void suspendUser_ShouldThrow_WhenAlreadySuspended() {
        User user = User.builder().userId(1L).isActive(false).role(User.Role.DRIVER).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminService.suspendUser(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already suspended");
    }

    @Test
    void suspendUser_ShouldThrow_WhenUserIsAdmin() {
        User admin = User.builder().userId(1L).isActive(true).role(User.Role.ADMIN).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminService.suspendUser(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot suspend an admin account");
    }

    @Test
    void reactivateUser_ShouldActivateUser() {
        User user = User.builder().userId(1L).isActive(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminService.reactivateUser(1L);

        assertThat(user.isActive()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void reactivateUser_ShouldThrow_WhenAlreadyActive() {
        User user = User.builder().userId(1L).isActive(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminService.reactivateUser(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already active");
    }

    @Test
    void deleteUser_ShouldDelete_WhenNotAdmin() {
        User user = User.builder().userId(1L).role(User.Role.DRIVER).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_ShouldThrow_WhenUserIsAdmin() {
        User admin = User.builder().userId(1L).role(User.Role.ADMIN).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminService.deleteUser(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete an admin account");
    }
}
