package com.parkease.parking.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "parking_spots", indexes = {
    @Index(name = "idx_spot_lot", columnList = "lot_id"),
    @Index(name = "idx_spot_status", columnList = "status"),
    @Index(name = "idx_spot_type", columnList = "spot_type")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long spotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private ParkingLot parkingLot;

    @Column(nullable = false)
    private String spotNumber;

    @Column(nullable = false)
    private int floor;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SpotType spotType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SpotStatus status;

    @Column(nullable = false)
    private Double pricePerHour;

    @Column(nullable = false)
    private boolean isEv = false;

    @Column(nullable = false)
    private boolean isHandicapped = false;

    @Column(nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum SpotType {
        COMPACT, STANDARD, LARGE, EV_ONLY, HANDICAPPED
    }

    public enum SpotStatus {
        AVAILABLE, RESERVED, OCCUPIED
    }
}
