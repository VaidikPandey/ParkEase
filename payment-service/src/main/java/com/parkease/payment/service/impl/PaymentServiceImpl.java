package com.parkease.payment.service.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.parkease.payment.domain.entity.Payment;
import com.parkease.payment.messaging.PaymentEventPublisher;
import com.parkease.payment.repository.PaymentRepository;
import com.parkease.payment.service.PaymentService;
import com.parkease.payment.web.dto.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @Override
    public PaymentResponse processPayment(PaymentRequest request, Long callerId, boolean isAdmin) {
        paymentRepository.findByBookingId(request.getBookingId())
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PAID)
                .ifPresent(p -> {
                    throw new IllegalStateException(
                            "Payment already completed for bookingId: " + request.getBookingId()
                            + " | TxnId: " + p.getTransactionId());
                });

        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .userId(callerId)
                .lotId(request.getLotId())
                .amount(request.getAmount())
                .mode(request.getMode())
                .status(Payment.PaymentStatus.PAID)
                .transactionId(generateTransactionId())
                .currency("INR")
                .description(request.getDescription())
                .paidAt(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment processed: txnId={} bookingId={} amount={} mode={}",
                saved.getTransactionId(), saved.getBookingId(), saved.getAmount(), saved.getMode());

        eventPublisher.publishPaymentEvent(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getByBookingId(Long bookingId, Long callerId, boolean isAdmin) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No payment found for bookingId: " + bookingId));
        enforceOwnership(payment, callerId, isAdmin);
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getByUserId(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public PaymentResponse refundPayment(RefundRequest request, Long callerId, boolean isAdmin) {
        Payment payment = paymentRepository.findByBookingId(request.getBookingId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No payment found for bookingId: " + request.getBookingId()));
        enforceOwnership(payment, callerId, isAdmin);

        if (payment.getStatus() == Payment.PaymentStatus.REFUNDED) {
            throw new IllegalStateException(
                    "Already refunded for bookingId: " + request.getBookingId());
        }
        if (payment.getStatus() != Payment.PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Cannot refund — payment status is: " + payment.getStatus());
        }

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setDescription("REFUND | Reason: " +
                (request.getReason() != null ? request.getReason() : "Booking cancellation"));

        Payment saved = paymentRepository.save(payment);
        log.info("Refund processed: bookingId={} amount={}", saved.getBookingId(), saved.getAmount());

        eventPublisher.publishRefundEvent(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(Long paymentId, Long callerId, boolean isAdmin) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));
        enforceOwnership(payment, callerId, isAdmin);
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateReceipt(Long bookingId, Long callerId, boolean isAdmin) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No payment found for bookingId: " + bookingId));
        enforceOwnership(payment, callerId, isAdmin);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont  = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,   BaseColor.DARK_GRAY);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD,   BaseColor.BLACK);
            Font labelFont  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   BaseColor.GRAY);
            Font valueFont  = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.BLACK);
            Font footerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY);

            doc.add(new Paragraph("ParkEase Smart Parking", titleFont));
            doc.add(new Paragraph("Payment Receipt", headerFont));
            doc.add(Chunk.NEWLINE);

            addRow(doc, labelFont, valueFont, "Transaction ID",  payment.getTransactionId());
            addRow(doc, labelFont, valueFont, "Booking ID",      String.valueOf(payment.getBookingId()));
            addRow(doc, labelFont, valueFont, "User ID",         String.valueOf(payment.getUserId()));
            addRow(doc, labelFont, valueFont, "Amount",          payment.getCurrency() + " " + String.format("%.2f", payment.getAmount()));
            addRow(doc, labelFont, valueFont, "Payment Mode",    payment.getMode().name());
            addRow(doc, labelFont, valueFont, "Status",          payment.getStatus().name());
            addRow(doc, labelFont, valueFont, "Paid At",         payment.getPaidAt() != null ? payment.getPaidAt().format(DISPLAY_FMT) : "N/A");

            if (payment.getRefundedAt() != null) {
                addRow(doc, labelFont, valueFont, "Refunded At", payment.getRefundedAt().format(DISPLAY_FMT));
            }
            if (payment.getDescription() != null && !payment.getDescription().isBlank()) {
                addRow(doc, labelFont, valueFont, "Note", payment.getDescription());
            }

            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Thank you for using ParkEase. Drive safe!", footerFont));
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Receipt generation failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueByLot(Long lotId, LocalDateTime from, LocalDateTime to) {
        Double revenue = (from != null && to != null)
                ? paymentRepository.sumAmountByLotIdAndDateRange(lotId, from, to)
                : paymentRepository.sumAmountByLotId(lotId);
        Long count = paymentRepository.countByLotId(lotId);

        return RevenueReportResponse.builder()
                .lotId(lotId)
                .totalRevenue(revenue != null ? revenue : 0.0)
                .totalTransactions(count != null ? count : 0L)
                .fromDate(from != null ? from.format(DISPLAY_FMT) : "ALL TIME")
                .toDate(to   != null ? to.format(DISPLAY_FMT)   : "ALL TIME")
                .currency("INR")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalPlatformRevenue(LocalDateTime from, LocalDateTime to) {
        Double rev = paymentRepository.sumTotalRevenueBetween(from, to);
        return rev != null ? rev : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAllTimePlatformRevenue() {
        Double rev = paymentRepository.sumTotalRevenue();
        return rev != null ? rev : 0.0;
    }

    private void addRow(Document doc, Font labelFont, Font valueFont, String label, String value)
            throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", labelFont));
        p.add(new Chunk(value != null ? value : "N/A", valueFont));
        doc.add(p);
    }

    private void enforceOwnership(Payment payment, Long callerId, boolean isAdmin) {
        if (!isAdmin && !payment.getUserId().equals(callerId)) {
            throw new AccessDeniedException("You can only access your own payments");
        }
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getPaymentId())
                .bookingId(p.getBookingId())
                .userId(p.getUserId())
                .lotId(p.getLotId())
                .amount(p.getAmount())
                .status(p.getStatus())
                .mode(p.getMode())
                .transactionId(p.getTransactionId())
                .currency(p.getCurrency())
                .description(p.getDescription())
                .paidAt(p.getPaidAt())
                .refundedAt(p.getRefundedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
