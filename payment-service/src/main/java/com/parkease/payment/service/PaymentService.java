package com.parkease.payment.service;

import com.parkease.payment.web.dto.*;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request, Long callerId, boolean isAdmin);
    PaymentResponse getByBookingId(Long bookingId, Long callerId, boolean isAdmin);
    List<PaymentResponse> getByUserId(Long userId);
    PaymentResponse refundPayment(RefundRequest request, Long callerId, boolean isAdmin);
    PaymentResponse getPaymentStatus(Long paymentId, Long callerId, boolean isAdmin);
    byte[] generateReceipt(Long bookingId, Long callerId, boolean isAdmin);
    RevenueReportResponse getRevenueByLot(Long lotId, LocalDateTime from, LocalDateTime to);
    Double getTotalPlatformRevenue(LocalDateTime from, LocalDateTime to);
    Double getAllTimePlatformRevenue();
}
