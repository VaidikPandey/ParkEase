package com.parkease.payment.web.resource;

import com.parkease.payment.service.PaymentService;
import com.parkease.payment.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing, refunds, receipts, and revenue reports")
public class PaymentResource {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    @Operation(summary = "Create a Razorpay order and return orderId + keyId")
    public ResponseEntity<RazorpayOrderResponse> createOrder(
            @Valid @RequestBody RazorpayOrderRequest request) {
        return ResponseEntity.ok(paymentService.createOrder(request));
    }

    @PostMapping
    @Operation(summary = "Process a payment for a booking")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("X-User-Id") Long callerId,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(request, callerId, isAdmin(auth)));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get payment by booking ID")
    public ResponseEntity<PaymentResponse> getByBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long callerId,
            Authentication auth) {
        return ResponseEntity.ok(paymentService.getByBookingId(bookingId, callerId, isAdmin(auth)));
    }

    @GetMapping("/{paymentId}/status")
    @Operation(summary = "Get payment status by payment ID")
    public ResponseEntity<PaymentResponse> getStatus(
            @PathVariable Long paymentId,
            @RequestHeader("X-User-Id") Long callerId,
            Authentication auth) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(paymentId, callerId, isAdmin(auth)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all payments for a user")
    public ResponseEntity<List<PaymentResponse>> getByUser(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long callerId,
            Authentication auth) {
        if (!isAdmin(auth) && !userId.equals(callerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(paymentService.getByUserId(userId));
    }

    @PostMapping("/refund")
    @Operation(summary = "Process a refund for a booking")
    public ResponseEntity<PaymentResponse> refund(
            @Valid @RequestBody RefundRequest request,
            @RequestHeader("X-User-Id") Long callerId,
            Authentication auth) {
        return ResponseEntity.ok(paymentService.refundPayment(request, callerId, isAdmin(auth)));
    }

    @GetMapping("/booking/{bookingId}/receipt")
    @Operation(summary = "Download PDF receipt for a booking")
    public ResponseEntity<byte[]> downloadReceipt(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long callerId,
            Authentication auth) {
        byte[] pdf = paymentService.generateReceipt(bookingId, callerId, isAdmin(auth));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("receipt-booking-" + bookingId + ".pdf")
                .build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @GetMapping("/admin/revenue/lot/{lotId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Revenue report for a lot — ADMIN / MANAGER")
    public ResponseEntity<RevenueReportResponse> getLotRevenue(
            @PathVariable Long lotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(paymentService.getRevenueByLot(lotId, from, to));
    }

    @GetMapping("/admin/revenue/platform")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Total platform revenue for a date range — ADMIN")
    public ResponseEntity<Double> getPlatformRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(paymentService.getTotalPlatformRevenue(from, to));
    }

    @GetMapping("/admin/revenue/platform/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "All-time total platform revenue — ADMIN")
    public ResponseEntity<Double> getAllTimeRevenue() {
        return ResponseEntity.ok(paymentService.getAllTimePlatformRevenue());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
