package com.parkease.analytics.messaging;

import com.parkease.analytics.config.RabbitMQConfig;
import com.parkease.analytics.domain.entity.BookingAnalytics;
import com.parkease.analytics.repository.BookingAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@Lazy(false)
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    private final BookingAnalyticsRepository repository;

    @RabbitListener(queues = RabbitMQConfig.ANALYTICS_QUEUE)
    public void handle(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            log.info("Analytics received event: type={}", eventType);

            if (eventType == null) return;

            switch (eventType) {
                case "booking.pending"    -> handlePending(event);
                case "booking.confirmed"  -> handleConfirmed(event);
                case "booking.checkin"    -> handleCheckIn(event);
                case "booking.checkout"   -> handleCheckOut(event);
                case "booking.cancelled"  -> handleCancelled(event);
                case "booking.expiry"     -> handleExpiry(event);
                case "payment.completed"  -> handlePaymentCompleted(event);
                default -> log.debug("Skipping unrelated event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing analytics event: {}", e.getMessage(), e);
        }
    }

    private void handlePending(Map<String, Object> event) {
        Long bookingId = toLong(event.get("bookingId"));
        if (bookingId == null || repository.findByBookingId(bookingId).isPresent()) return;

        BookingAnalytics record = baseRecord(event)
                .status(BookingAnalytics.BookingStatus.CONFIRMED)
                .confirmedAt(toDateTime(event.get("occurredAt")))
                .build();

        repository.save(record);
        log.info("Analytics: booking {} pending", bookingId);
    }

    private void handleConfirmed(Map<String, Object> event) {
        Long bookingId = toLong(event.get("bookingId"));
        if (bookingId == null) return;

        BookingAnalytics record = repository.findByBookingId(bookingId)
                .orElseGet(() -> baseRecord(event).build());
        record.setStatus(BookingAnalytics.BookingStatus.CONFIRMED);
        if (record.getConfirmedAt() == null) {
            record.setConfirmedAt(toDateTime(event.get("occurredAt")));
        }

        repository.save(record);
        log.info("Analytics: booking {} confirmed", bookingId);
    }

    private void handleCheckIn(Map<String, Object> event) {
        Long bookingId = toLong(event.get("bookingId"));
        if (bookingId == null) return;

        BookingAnalytics record = repository.findByBookingId(bookingId)
                .orElseGet(() -> baseRecord(event).build());
        record.setStatus(BookingAnalytics.BookingStatus.ACTIVE);
        record.setCheckinAt(toDateTime(event.get("occurredAt")));
        if (record.getConfirmedAt() == null) {
            record.setConfirmedAt(toDateTime(event.get("startTime")));
        }
        repository.save(record);
        log.info("Analytics: booking {} checked in", bookingId);
    }

    private void handleCheckOut(Map<String, Object> event) {
        Long bookingId = toLong(event.get("bookingId"));
        repository.findByBookingId(bookingId).ifPresent(record -> {
            LocalDateTime checkoutAt = toDateTime(event.get("occurredAt"));
            record.setStatus(BookingAnalytics.BookingStatus.COMPLETED);
            record.setCheckoutAt(checkoutAt);
            record.setTotalFare(toDouble(event.get("totalFare")));

            if (record.getCheckinAt() != null && checkoutAt != null) {
                long minutes = java.time.Duration.between(record.getCheckinAt(), checkoutAt).toMinutes();
                record.setDurationMinutes(minutes);
            }

            repository.save(record);
            log.info("Analytics: booking {} checked out", bookingId);
        });
    }

    private void handleCancelled(Map<String, Object> event) {
        Long bookingId = toLong(event.get("bookingId"));
        if (bookingId == null) return;

        BookingAnalytics record = repository.findByBookingId(bookingId)
                .orElseGet(() -> baseRecord(event).build());
        record.setStatus(BookingAnalytics.BookingStatus.CANCELLED);
        record.setCancelledAt(toDateTime(event.get("occurredAt")));
        repository.save(record);
        log.info("Analytics: booking {} cancelled", bookingId);
    }

    private void handleExpiry(Map<String, Object> event) {
        Long bookingId = toLong(event.get("bookingId"));
        if (bookingId == null) return;

        BookingAnalytics record = repository.findByBookingId(bookingId)
                .orElseGet(() -> baseRecord(event).build());
        record.setStatus(BookingAnalytics.BookingStatus.EXPIRED);
        record.setCancelledAt(toDateTime(event.get("occurredAt")));
        repository.save(record);
        log.info("Analytics: booking {} expired", bookingId);
    }

    private void handlePaymentCompleted(Map<String, Object> event) {
        Long bookingId = toLong(event.get("bookingId"));
        repository.findByBookingId(bookingId).ifPresent(record -> {
            Double amount = toDouble(event.get("amount"));
            if (amount != null) {
                record.setTotalFare(amount);
                repository.save(record);
            }
        });
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    private BookingAnalytics.BookingAnalyticsBuilder baseRecord(Map<String, Object> event) {
        return BookingAnalytics.builder()
                .bookingId(toLong(event.get("bookingId")))
                .driverId(toLong(event.get("driverId")))
                .spotId(toLong(event.get("spotId")))
                .lotId(toLong(event.get("lotId")))
                .spotNumber((String) event.get("spotNumber"))
                .bookingType((String) event.get("bookingType"))
                .totalFare(toDouble(event.get("totalFare")));
    }

    private LocalDateTime toDateTime(Object val) {
        if (val == null) return LocalDateTime.now();
        if (val instanceof LocalDateTime) return (LocalDateTime) val;
        try { return LocalDateTime.parse(val.toString()); } catch (Exception e) { return LocalDateTime.now(); }
    }
}
