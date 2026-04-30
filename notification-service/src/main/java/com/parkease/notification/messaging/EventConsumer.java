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
        Long   driverId    = toLong(event.get("driverId"));
        Long   bookingId   = toLong(event.get("bookingId"));
        String email       = (String) event.get("driverEmail");
        String spotNumber  = str(event.get("spotNumber"));
        String plate       = str(event.get("vehiclePlate"));
        String startTime   = str(event.get("startTime"));
        String endTime     = str(event.get("endTime"));
        Object pricePerHour = event.get("pricePerHour");
        Object fare         = event.get("totalFare");
        String bookingType  = str(event.get("bookingType"));

        String title   = "Booking Confirmed — #" + bookingId;
        String message = "Your parking booking #" + bookingId + " (Spot " + spotNumber + ") is confirmed.";

        notificationService.createFromEvent(driverId,
                Notification.NotificationType.BOOKING, title, message, bookingId, "BOOKING");

        if (email != null) {
            notificationService.sendHtmlEmail(email, "[ParkEase] Booking Confirmed — #" + bookingId,
                    buildReceiptHtml(bookingId, spotNumber, plate, startTime, endTime,
                            pricePerHour, fare, bookingType, "confirmed"));
        }
    }

    private void handleCheckIn(Map<String, Object> event) {
        Long   driverId  = toLong(event.get("driverId"));
        Long   bookingId = toLong(event.get("bookingId"));
        String email     = (String) event.get("driverEmail");
        String title     = "Checked In — Booking #" + bookingId;
        String message   = "You have checked in for booking #" + bookingId + ". Enjoy your parking!";
        notificationService.createFromEvent(driverId,
                Notification.NotificationType.CHECKIN, title, message, bookingId, "BOOKING");
        if (email != null) notificationService.sendEmail(email, "[ParkEase] " + title, message);
    }

    private void handleCheckOut(Map<String, Object> event) {
        Long   driverId    = toLong(event.get("driverId"));
        Long   bookingId   = toLong(event.get("bookingId"));
        String email       = (String) event.get("driverEmail");
        String spotNumber  = str(event.get("spotNumber"));
        String plate       = str(event.get("vehiclePlate"));
        String startTime   = str(event.get("startTime"));
        String endTime     = str(event.get("endTime"));
        Object pricePerHour = event.get("pricePerHour");
        Object fare         = event.get("totalFare");
        String bookingType  = str(event.get("bookingType"));

        String title   = "Payment Receipt — Booking #" + bookingId;
        String message = "Checked out for booking #" + bookingId + ". Total fare: ₹" + fare + ". Thank you!";

        notificationService.createFromEvent(driverId,
                Notification.NotificationType.CHECKOUT, title, message, bookingId, "BOOKING");

        if (email != null) {
            notificationService.sendHtmlEmail(email, "[ParkEase] Payment Receipt — #" + bookingId,
                    buildReceiptHtml(bookingId, spotNumber, plate, startTime, endTime,
                            pricePerHour, fare, bookingType, "checkout"));
        }
    }

    private void handleBookingCancelled(Map<String, Object> event) {
        Long   driverId  = toLong(event.get("driverId"));
        Long   bookingId = toLong(event.get("bookingId"));
        String email     = (String) event.get("driverEmail");
        String title     = "Booking Cancelled — #" + bookingId;
        String message   = "Your booking #" + bookingId + " has been cancelled.";
        notificationService.createFromEvent(driverId,
                Notification.NotificationType.BOOKING, title, message, bookingId, "BOOKING");
        if (email != null) notificationService.sendEmail(email, "[ParkEase] " + title, message);
    }

    private void handleExpiryReminder(Map<String, Object> event) {
        Long   driverId  = toLong(event.get("driverId"));
        Long   bookingId = toLong(event.get("bookingId"));
        String email     = (String) event.get("driverEmail");
        String title     = "Parking Expiring Soon — #" + bookingId;
        String message   = "Your parking booking #" + bookingId + " expires in 15 minutes. Please check out or extend your stay.";
        notificationService.createFromEvent(driverId,
                Notification.NotificationType.EXPIRY, title, message, bookingId, "BOOKING");
        if (email != null) notificationService.sendEmail(email, "[ParkEase] " + title, message);
    }

    private void handleExpiry(Map<String, Object> event) {
        Long   driverId  = toLong(event.get("driverId"));
        Long   bookingId = toLong(event.get("bookingId"));
        String email     = (String) event.get("driverEmail");
        String title     = "Booking Expired — #" + bookingId;
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

    private String str(Object val) { return val == null ? "—" : val.toString(); }

    private String buildReceiptHtml(Long bookingId, String spotNumber, String plate,
                                     String startTime, String endTime,
                                     Object pricePerHour, Object totalFare,
                                     String bookingType, String stage) {

        boolean isCheckout = "checkout".equals(stage);
        String headerColor = isCheckout ? "#16a34a" : "#1d9bf0";
        String headerLabel = isCheckout ? "Payment Receipt" : "Booking Confirmation";
        String headerIcon  = isCheckout ? "✅" : "🎫";

        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"/></head>
        <body style="margin:0;padding:0;background:#0a0a0a;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0a0a0a;padding:32px 16px;">
            <tr><td align="center">
              <table width="560" cellpadding="0" cellspacing="0" style="background:#111;border:1px solid #222;border-radius:16px;overflow:hidden;max-width:560px;width:100%%;">

                <!-- Header -->
                <tr>
                  <td style="background:%s;padding:28px 32px;text-align:center;">
                    <div style="font-size:32px;margin-bottom:8px;">%s</div>
                    <h1 style="margin:0;font-size:22px;font-weight:800;color:#fff;letter-spacing:-.3px;">%s</h1>
                    <p style="margin:6px 0 0;font-size:14px;color:rgba(255,255,255,.75);">ParkEase · Booking #%s</p>
                  </td>
                </tr>

                <!-- Body -->
                <tr>
                  <td style="padding:28px 32px;">

                    <!-- Details table -->
                    <table width="100%%" cellpadding="0" cellspacing="0"
                           style="background:#0f0f0f;border:1px solid #222;border-radius:12px;overflow:hidden;margin-bottom:20px;">
                      %s
                      %s
                      %s
                      %s
                      %s
                      %s
                    </table>

                    %s

                    <p style="font-size:12px;color:#555;text-align:center;margin:24px 0 0;line-height:1.6;">
                      This is an automated receipt from ParkEase.<br/>
                      Please do not reply to this email.
                    </p>
                  </td>
                </tr>

                <!-- Footer -->
                <tr>
                  <td style="padding:16px 32px;border-top:1px solid #1e1e1e;text-align:center;">
                    <p style="margin:0;font-size:12px;color:#444;">© 2026 ParkEase. All rights reserved.</p>
                  </td>
                </tr>

              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.formatted(
                headerColor, headerIcon, headerLabel, bookingId,
                row("Spot Number", spotNumber, false),
                row("Vehicle Plate", plate, true),
                row("Booking Type", bookingType, false),
                row("From", formatTime(startTime), true),
                row("To", formatTime(endTime), false),
                row("Price per Hour", "₹" + pricePerHour, true),
                isCheckout
                    ? totalFareBlock(totalFare)
                    : "<p style=\"font-size:13px;color:#71767b;text-align:center;margin:0;\">Total fare will be calculated on checkout.</p>"
        );
    }

    private String row(String label, String value, boolean shaded) {
        String bg = shaded ? "background:#161616;" : "";
        return """
            <tr style="%s">
              <td style="padding:12px 16px;font-size:12px;color:#71767b;font-weight:600;width:40%%;border-bottom:1px solid #1e1e1e;">%s</td>
              <td style="padding:12px 16px;font-size:13px;color:#e7e9ea;font-weight:700;border-bottom:1px solid #1e1e1e;">%s</td>
            </tr>
            """.formatted(bg, label, value);
    }

    private String totalFareBlock(Object fare) {
        return """
            <div style="background:#0d2818;border:1px solid #16a34a33;border-radius:12px;padding:16px 20px;text-align:center;margin-top:4px;">
              <p style="margin:0 0 4px;font-size:12px;color:#4ade80;font-weight:600;text-transform:uppercase;letter-spacing:.06em;">Total Charged</p>
              <p style="margin:0;font-size:28px;font-weight:800;color:#4ade80;">₹%s</p>
            </div>
            """.formatted(fare);
    }

    private String formatTime(String iso) {
        if (iso == null || iso.equals("—")) return "—";
        try {
            return iso.replace("T", " ").substring(0, 16);
        } catch (Exception e) { return iso; }
    }
}
