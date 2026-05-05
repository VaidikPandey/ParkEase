package com.parkease.notification.messaging;

import com.parkease.notification.domain.entity.Notification;
import com.parkease.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EventConsumer eventConsumer;

    @Test
    void handle_ShouldProcessBookingConfirmed() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "booking.confirmed");
        event.put("driverId", 1);
        event.put("bookingId", 101);
        event.put("driverEmail", "test@test.com");
        event.put("spotNumber", "A1");

        eventConsumer.handle(event);

        verify(notificationService).createFromEvent(eq(1L), eq(Notification.NotificationType.BOOKING), anyString(), anyString(), eq(101L), eq("BOOKING"));
        verify(notificationService).sendHtmlEmail(eq("test@test.com"), anyString(), anyString());
    }

    @Test
    void handle_ShouldProcessCheckIn() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "booking.checkin");
        event.put("driverId", 1);
        event.put("bookingId", 101);
        event.put("driverEmail", "test@test.com");

        eventConsumer.handle(event);

        verify(notificationService).createFromEvent(eq(1L), eq(Notification.NotificationType.CHECKIN), anyString(), anyString(), eq(101L), eq("BOOKING"));
        verify(notificationService).sendEmail(eq("test@test.com"), anyString(), anyString());
    }
}
