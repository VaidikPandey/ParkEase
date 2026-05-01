package com.parkease.payment.web.dto;

import com.parkease.payment.domain.entity.Payment;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long paymentId;
    private Long bookingId;
    private Long userId;
    private Long lotId;
    private Double amount;
    private Payment.PaymentStatus status;
    private Payment.PaymentMode mode;
    private String transactionId;
    private String razorpayPaymentId;
    private String currency;
    private String description;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
