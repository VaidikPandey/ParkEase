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
                case "booking.reminder"   -> handleExpiryReminder(event);
                case "payment.completed"  -> handlePaymentCompleted(event);
                case "payment.refunded"        -> handleRefundProcessed(event);
                case "lot.capacity.threshold"  -> handleCapacityThreshold(event);
                default -> log.warn("Unhandled event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
        }
    }

    private void handleBookingConfirmed(Map<String, Object> event) {
        Long driverId    = toLong(event.get("driverId"));
        Long bookingId   = toLong(event.get("bookingId"));
        String email     = (String) event.get("driverEmail");
        String title     = "Booking Confirmed";
        String message   = "Your parking booking #" + bookingId + " is confirmed. See you at the lot!";
        notificationService.createFromEvent(driverId,
                Notification.NotificationType.BOOKING, title, message, bookingId, "BOOKING");
        if (email != null) notificationService.sendEmail(email, "[ParkEase] " + title, message);
    }

    private void handleCheckIn(Map<String, Object> event) {
        Long driverId    = toLong(event.get("driverId"));
        Long bookingId   = toLong(event.get("bookingId"));
        String email     = (String) event.get("driverEmail");
        String title     = "Check-In Successful";
        String message   = "You have checked in for booking #" + bookingId + ". Enjoy your parking!";
        notificationService.createFromEvent(driverId,
                Notification.NotificationType.CHECKIN, title, message, bookingId, "BOOKING");
        if (email != null) notificationService.sendEmail(email, "[ParkEase] " + title, message);
    }

    private void handleCheckOut(Map<String, Object> event) {
        Long driverId    = toLong(event.get("driverId"));
        Long bookingId   = toLong(event.get("bookingId"));
        String email     = (String) event.get("driverEmail");
        Object fare      = event.get("totalFare");
        String title     = "Check-Out Complete";
        String message   = "Checked out for booking #" + bookingId + ". Total fare: ₹" + fare + ". Thank you!";
        notificationService.createFromEvent(driverId,
                Notification.NotificationType.CHECKOUT, title, message, bookingId, "BOOKING");
        if (email != null) notificationService.sendEmail(email, "[ParkEase] " + title, message);
    }

    private void handleBookingCancelled(Map<String, Object> event) {
        Long driverId    = toLong(event.get("driverId"));
        Long bookingId   = toLong(event.get("bookingId"));
        String email     = (String) event.get("driverEmail");
        String title     = "Booking Cancelled";
        String message   = "Your booking #" + bookingId + " has been cancelled.";
        notificationService.createFromEvent(driverId,
                Notification.NotificationType.BOOKING, title, message, bookingId, "BOOKING");
        if (email != null) notificationService.sendEmail(email, "[ParkEase] " + title, message);
    }

    private void handleExpiryReminder(Map<String, Object> event) {
        Long driverId  = toLong(event.get("driverId"));
        Long bookingId = toLong(event.get("bookingId"));
        String email   = (String) event.get("driverEmail");
        String title   = "Parking Expiring Soon";
        String message = "Your parking booking #" + bookingId + " expires in 15 minutes. Please check out or extend your stay.";
        notificationService.createFromEvent(driverId,
                Notification.NotificationType.EXPIRY, title, message, bookingId, "BOOKING");
        if (email != null) notificationService.sendEmail(email, "[ParkEase] " + title, message);
    }

    private void handleExpiry(Map<String, Object> event) {
        Long driverId    = toLong(event.get("driverId"));
        Long bookingId   = toLong(event.get("bookingId"));
        String email     = (String) event.get("driverEmail");
        String title     = "Booking Expired";
        String message   = "Booking #" + bookingId + " expired — no check-in within the grace period.";
        notificationService.createFromEvent(driverId,
                Notification.NotificationType.EXPIRY, title, message, bookingId, "BOOKING");
        if (email != null) notificationService.sendEmail(email, "[ParkEase] " + title, message);
    }

    private void handlePaymentCompleted(Map<String, Object> event) {
        Long userId      = toLong(event.get("userId"));
        Long bookingId   = toLong(event.get("bookingId"));
        Object amount    = event.get("amount");
        String txnId     = (String) event.get("transactionId");
        String email     = (String) event.get("driverEmail");
        String title     = "Payment Received";
        String message   = "Payment of ₹" + amount + " for booking #" + bookingId + " confirmed. TxnID: " + txnId;
        notificationService.createFromEvent(userId,
                Notification.NotificationType.PAYMENT, title, message, bookingId, "PAYMENT");
        if (email != null) notificationService.sendEmail(email, "[ParkEase] " + title, message);
    }

    private void handleRefundProcessed(Map<String, Object> event) {
        Long userId      = toLong(event.get("userId"));
        Long bookingId   = toLong(event.get("bookingId"));
        Object amount    = event.get("amount");
        String email     = (String) event.get("driverEmail");
        String title     = "Refund Processed";
        String message   = "A refund of ₹" + amount + " for booking #" + bookingId + " has been processed.";
        notificationService.createFromEvent(userId,
                Notification.NotificationType.REFUND, title, message, bookingId, "PAYMENT");
        if (email != null) notificationService.sendEmail(email, "[ParkEase] " + title, message);
    }

    private void handleCapacityThreshold(Map<String, Object> event) {
        Long   managerId      = toLong(event.get("managerId"));
        String lotName        = (String) event.get("lotName");
        Object threshold      = event.get("threshold");
        Object occupancy      = event.get("occupancyPercent");
        Object occupied       = event.get("occupiedSpots");
        Object total          = event.get("totalSpots");
        Long   lotId          = toLong(event.get("lotId"));

        String title   = lotName + " is " + threshold + "% full";
        String message = lotName + " has reached " + threshold + "% capacity ("
                + occupied + "/" + total + " spots occupied, " + occupancy + "%).";

        if (managerId != null) {
            notificationService.createFromEvent(managerId,
                    Notification.NotificationType.SYSTEM, title, message, lotId, "LOT");
        }
        log.info("Capacity notification sent: lot={} threshold={}%", lotName, threshold);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }
}
