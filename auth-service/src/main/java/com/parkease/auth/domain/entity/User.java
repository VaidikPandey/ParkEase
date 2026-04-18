package com.parkease.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_role",  columnList = "role")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String passwordHash;

    @Column
    private String phone;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column
    private String vehiclePlate;       // quick-ref for single-vehicle drivers

    @Column(nullable = false)
    private boolean isActive = true;

    @Column
    private String profilePicUrl;

    @Column
    private String oauthProvider;      // "google" | null

    @Column
    private String oauthProviderId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum Role {
        DRIVER, MANAGER, ADMIN
    }
}
