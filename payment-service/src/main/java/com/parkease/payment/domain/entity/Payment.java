package com.parkease.payment.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_booking", columnList = "bookingId"),
        @Index(name = "idx_payment_user",    columnList = "userId"),
        @Index(name = "idx_payment_status",  columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Long userId;

    private Long lotId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMode mode;

    @Column(unique = true)
    private String transactionId;

    @Column(length = 10)
    private String currency;

    @Column(length = 500)
    private String description;

    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.currency == null) this.currency = "INR";
    }

    public enum PaymentStatus {
        PENDING, PAID, REFUNDED, FAILED
    }

    public enum PaymentMode {
        CARD, UPI, WALLET, CASH
    }
}
