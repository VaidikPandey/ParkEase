package com.parkease.auth.repository;

import com.parkease.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByRole(User.Role role);
    Optional<User> findByOauthProviderAndOauthProviderId(String provider, String providerId);
    Optional<User> findByPhone(String phone);
}
