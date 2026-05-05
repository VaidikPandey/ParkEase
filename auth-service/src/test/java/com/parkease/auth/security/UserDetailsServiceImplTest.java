package com.parkease.auth.security;

import com.parkease.auth.domain.entity.User;
import com.parkease.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenActiveUserFound() {
        User user = User.builder()
                .email("driver@test.com")
                .passwordHash("hashed")
                .role(User.Role.DRIVER)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("driver@test.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("driver@test.com");

        assertThat(details.getUsername()).isEqualTo("driver@test.com");
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_DRIVER"));
    }

    @Test
    void loadUserByUsername_ShouldThrow_WhenUserNotFound() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nobody@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_ShouldThrow_WhenAccountDeactivated() {
        User user = User.builder()
                .email("suspended@test.com")
                .role(User.Role.DRIVER)
                .isActive(false)
                .build();
        when(userRepository.findByEmail("suspended@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("suspended@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("deactivated");
    }
}
