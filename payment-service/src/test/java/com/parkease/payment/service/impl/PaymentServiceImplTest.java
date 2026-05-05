package com.parkease.payment.service.impl;

import com.parkease.payment.domain.entity.Payment;
import com.parkease.payment.messaging.PaymentEventPublisher;
import com.parkease.payment.repository.PaymentRepository;
import com.parkease.payment.web.dto.PaymentRequest;
import com.parkease.payment.web.dto.PaymentResponse;
import com.parkease.payment.web.dto.RefundRequest;
import com.parkease.payment.web.dto.RevenueReportResponse;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentEventPublisher eventPublisher;
    @Mock
    private RazorpayClient razorpayClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void processPayment_ShouldSavePaymentAndPublishEvent() {
        // Arrange
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(1L);
        request.setLotId(10L);
        request.setAmount(100.0);
        request.setMode(Payment.PaymentMode.UPI);

        when(paymentRepository.findByBookingId(anyLong())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        PaymentResponse response = paymentService.processPayment(request, 1L, false);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
        verify(paymentRepository).save(any(Payment.class));
        verify(eventPublisher).publishPaymentEvent(any(Payment.class));
    }

    @Test
    void refundPayment_ShouldUpdateStatusToRefunded() {
        // Arrange
        RefundRequest request = new RefundRequest();
        request.setBookingId(1L);
        request.setReason("Customer request");

        Payment payment = Payment.builder()
                .bookingId(1L)
                .userId(1L)
                .status(Payment.PaymentStatus.PAID)
                .amount(100.0)
                .build();

        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        PaymentResponse response = paymentService.refundPayment(request, 1L, false);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        assertThat(payment.getDescription()).contains("Customer request");
        verify(eventPublisher).publishRefundEvent(any(Payment.class));
    }

    @Test
    void processPayment_ShouldThrow_WhenAlreadyPaid() {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(1L);
        request.setAmount(100.0);
        request.setMode(Payment.PaymentMode.UPI);

        Payment existing = Payment.builder()
                .bookingId(1L)
                .status(Payment.PaymentStatus.PAID)
                .transactionId("TXN-123")
                .build();

        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(existing));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> paymentService.processPayment(request, 1L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment already completed");
    }

    @Test
    void getByBookingId_ShouldReturnPayment_WhenOwnerCalls() {
        Payment payment = Payment.builder()
                .bookingId(1L)
                .userId(1L)
                .status(Payment.PaymentStatus.PAID)
                .amount(100.0)
                .build();

        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getByBookingId(1L, 1L, false);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
    }

    @Test
    void getByBookingId_ShouldThrow_WhenNotOwner() {
        Payment payment = Payment.builder()
                .bookingId(1L)
                .userId(99L)
                .build();

        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> paymentService.getByBookingId(1L, 1L, false))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void getByUserId_ShouldReturnList() {
        Payment payment = Payment.builder().bookingId(1L).userId(1L).status(Payment.PaymentStatus.PAID).build();
        when(paymentRepository.findByUserId(1L)).thenReturn(List.of(payment));

        List<PaymentResponse> result = paymentService.getByUserId(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void refundPayment_ShouldThrow_WhenAlreadyRefunded() {
        RefundRequest request = new RefundRequest();
        request.setBookingId(1L);

        Payment payment = Payment.builder()
                .bookingId(1L)
                .userId(1L)
                .status(Payment.PaymentStatus.REFUNDED)
                .build();

        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> paymentService.refundPayment(request, 1L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Already refunded");
    }

    @Test
    void getRevenueByLot_ShouldReturnReport() {
        when(paymentRepository.sumAmountByLotIdAndDateRange(anyLong(), any(), any())).thenReturn(500.0);
        when(paymentRepository.countByLotId(anyLong())).thenReturn(5L);

        RevenueReportResponse response = paymentService.getRevenueByLot(
                1L, java.time.LocalDateTime.now().minusDays(7), java.time.LocalDateTime.now());

        assertThat(response.getTotalRevenue()).isEqualTo(500.0);
        assertThat(response.getTotalTransactions()).isEqualTo(5L);
    }

    @Test
    void getTotalPlatformRevenue_ShouldReturnSum() {
        when(paymentRepository.sumTotalRevenueBetween(any(), any())).thenReturn(1000.0);

        Double revenue = paymentService.getTotalPlatformRevenue(
                java.time.LocalDateTime.now().minusDays(30), java.time.LocalDateTime.now());

        assertThat(revenue).isEqualTo(1000.0);
    }

    @Test
    void getAllTimePlatformRevenue_ShouldReturnSum() {
        when(paymentRepository.sumTotalRevenue()).thenReturn(9999.0);

        Double revenue = paymentService.getAllTimePlatformRevenue();

        assertThat(revenue).isEqualTo(9999.0);
    }
}
