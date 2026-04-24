package com.parkease.notification.messaging;

import com.parkease.notification.config.RabbitMQConfig;
import com.parkease.notification.domain.entity.Notification;
import com.parkease.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Lazy(false)
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handle(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            log.info("Received event: type={}", eventType);

            if (eventType == null) {
                log.warn("Event missing eventType, skipping");
                return;
            }

            switch (eventType) {
                case "booking.confirmed"  -> handleBookingConfirmed(event);
                case "booking.checkin"    -> handleCheckIn(event);
                case "booking.checkout"   -> handleCheckOut(event);
                case "booking.cancelled"  -> handleBookingCancelled(event);
                case "booking.expiry"     -> handleExpiry(event);
                case "payment.completed"  -> handlePaymentCompleted(event);
                case "payment.refunded"   -> handleRefundProcessed(event);
                default -> log.warn("Unhandled event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
        }
    }

    private void handleBookingConfirmed(Map<String, Object> event) {
        Long driverId  = toLong(event.get("driverId"));
        Long bookingId = toLong(event.get("bookingId"));
        notificationService.createFromEvent(driverId,
                Notification.NotificationType.BOOKING,
                "Booking Confirmed",
                "Your parking booking #" + bookingId + " is confirmed. See you at the lot!",
                bookingId, "BOOKING");
    }

    private void handleCheckIn(Map<String, Object> event) {
        Long driverId  = toLong(event.get("driverId"));
        Long bookingId = toLong(event.get("bookingId"));
        notificationService.createFromEvent(driverId,
                Notification.NotificationType.CHECKIN,
                "Check-In Successful",
                "You have checked in for booking #" + bookingId + ". Enjoy your parking!",
                bookingId, "BOOKING");
    }

    private void handleCheckOut(Map<String, Object> event) {
        Long driverId  = toLong(event.get("driverId"));
        Long bookingId = toLong(event.get("bookingId"));
        Object fare    = event.get("totalFare");
        notificationService.createFromEvent(driverId,
                Notification.NotificationType.CHECKOUT,
                "Check-Out Complete",
                "Checked out for booking #" + bookingId + ". Total fare: ₹" + fare + ". Thank you!",
                bookingId, "BOOKING");
    }

    private void handleBookingCancelled(Map<String, Object> event) {
        Long driverId  = toLong(event.get("driverId"));
        Long bookingId = toLong(event.get("bookingId"));
        notificationService.createFromEvent(driverId,
                Notification.NotificationType.BOOKING,
                "Booking Cancelled",
                "Your booking #" + bookingId + " has been cancelled.",
                bookingId, "BOOKING");
    }

    private void handleExpiry(Map<String, Object> event) {
        Long driverId  = toLong(event.get("driverId"));
        Long bookingId = toLong(event.get("bookingId"));
        notificationService.createFromEvent(driverId,
                Notification.NotificationType.EXPIRY,
                "Booking Expired",
                "Booking #" + bookingId + " expired — no check-in within the grace period.",
                bookingId, "BOOKING");
    }

    private void handlePaymentCompleted(Map<String, Object> event) {
        Long userId    = toLong(event.get("userId"));
        Long bookingId = toLong(event.get("bookingId"));
        Object amount  = event.get("amount");
        String txnId   = (String) event.get("transactionId");
        notificationService.createFromEvent(userId,
                Notification.NotificationType.PAYMENT,
                "Payment Received",
                "Payment of ₹" + amount + " for booking #" + bookingId + " confirmed. TxnID: " + txnId,
                bookingId, "PAYMENT");
    }

    private void handleRefundProcessed(Map<String, Object> event) {
        Long userId    = toLong(event.get("userId"));
        Long bookingId = toLong(event.get("bookingId"));
        Object amount  = event.get("amount");
        notificationService.createFromEvent(userId,
                Notification.NotificationType.REFUND,
                "Refund Processed",
                "A refund of ₹" + amount + " for booking #" + bookingId + " has been processed.",
                bookingId, "PAYMENT");
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }
}
