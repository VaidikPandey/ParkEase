package com.parkease.analytics.messaging;

import com.parkease.analytics.domain.entity.BookingAnalytics;
import com.parkease.analytics.repository.BookingAnalyticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    @Mock
    private BookingAnalyticsRepository repository;

    @InjectMocks
    private EventConsumer eventConsumer;

    @Test
    void handle_ShouldCreateRecord_WhenBookingConfirmed() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "booking.confirmed");
        event.put("bookingId", 101);
        event.put("driverId", 1);
        event.put("spotId", 10);
        event.put("lotId", 1);

        when(repository.findByBookingId(101L)).thenReturn(Optional.empty());

        eventConsumer.handle(event);

        verify(repository).save(any(BookingAnalytics.class));
    }

    @Test
    void handle_ShouldUpdateStatus_WhenCheckIn() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "booking.checkin");
        event.put("bookingId", 101L);

        BookingAnalytics record = new BookingAnalytics();
        when(repository.findByBookingId(101L)).thenReturn(Optional.of(record));

        eventConsumer.handle(event);

        assertThat(record.getStatus()).isEqualTo(BookingAnalytics.BookingStatus.ACTIVE);
        verify(repository).save(record);
    }

    private org.assertj.core.api.AbstractAssert<?, ?> assertThat(Object actual) {
        return org.assertj.core.api.Assertions.assertThat(actual);
    }
}
