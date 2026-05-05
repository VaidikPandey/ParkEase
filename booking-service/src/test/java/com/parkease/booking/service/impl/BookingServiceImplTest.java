package com.parkease.booking.service.impl;

import com.parkease.booking.domain.entity.Booking;
import com.parkease.booking.repository.BookingRepository;
import com.parkease.booking.web.dto.request.CancelBookingRequest;
import com.parkease.booking.web.dto.request.CreateBookingRequest;
import com.parkease.booking.web.dto.request.ExtendBookingRequest;
import com.parkease.booking.web.dto.response.BookingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "lockTimeoutSeconds", 10L);
    }

    @Test
    void createBooking_ShouldSuccess_WhenNoConflictsAndLockAcquired() throws InterruptedException {
        // Arrange
        Long driverId = 1L;
        CreateBookingRequest request = new CreateBookingRequest();
        request.setSpotId(101L);
        request.setLotId(1L);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setPricePerHour(20.0);
        request.setDriverEmail("driver@test.com");
        request.setBookingType(Booking.BookingType.PRE_BOOKING);

        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(bookingRepository.findConflictingBookings(any(), any(), any())).thenReturn(new ArrayList<>());
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setBookingId(1L);
            return b;
        });

        // Act
        BookingResponse response = bookingService.createBooking(driverId, request);

        // Assert
        assertThat(response).isNotNull();
        verify(bookingRepository).save(any(Booking.class));
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(lock).unlock();
    }

    @Test
    void createBooking_ShouldThrowException_WhenTimeWindowIsInvalid() {
        // Arrange
        CreateBookingRequest request = new CreateBookingRequest();
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setEndTime(LocalDateTime.now().plusHours(1));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void checkIn_ShouldUpdateStatusAndCheckInTime() {
        // Arrange
        Long bookingId = 1L;
        Long driverId = 10L;
        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .driverId(driverId)
                .status(Booking.BookingStatus.CONFIRMED)
                .bookingType(Booking.BookingType.PRE_BOOKING)
                .driverEmail("driver@test.com")
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(java.util.Optional.of(booking));

        // Act
        BookingResponse response = bookingService.checkIn(bookingId, driverId);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Booking.BookingStatus.CHECKED_IN.name());
        assertThat(booking.getCheckInTime()).isNotNull();
        verify(bookingRepository).save(booking);
    }

    @Test
    void checkOut_ShouldCalculateFareAndSetStatus() {
        // Arrange
        Long bookingId = 1L;
        Long driverId = 10L;
        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .driverId(driverId)
                .status(Booking.BookingStatus.CHECKED_IN)
                .bookingType(Booking.BookingType.PRE_BOOKING)
                .checkInTime(LocalDateTime.now().minusHours(2))
                .pricePerHour(10.0)
                .driverEmail("driver@test.com")
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(java.util.Optional.of(booking));

        // Act
        BookingResponse response = bookingService.checkOut(bookingId, driverId);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Booking.BookingStatus.CHECKED_OUT.name());
        assertThat(booking.getTotalFare()).isEqualTo(20.0);
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBooking_ShouldSetCancelledStatus_WhenValid() {
        Long bookingId = 1L;
        Long driverId = 10L;
        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .driverId(driverId)
                .status(Booking.BookingStatus.CONFIRMED)
                .bookingType(Booking.BookingType.PRE_BOOKING)
                .driverEmail("driver@test.com")
                .build();

        CancelBookingRequest request = new CancelBookingRequest();
        request.setReason(CancelBookingRequest.CancellationReason.CHANGE_OF_PLANS);

        when(bookingRepository.findById(bookingId)).thenReturn(java.util.Optional.of(booking));

        BookingResponse response = bookingService.cancelBooking(bookingId, driverId, request);

        assertThat(response.getStatus()).isEqualTo(Booking.BookingStatus.CANCELLED.name());
        verify(bookingRepository).save(booking);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void cancelBooking_ShouldThrow_WhenInvalidOtherReason() {
        CancelBookingRequest request = new CancelBookingRequest();
        request.setReason(CancelBookingRequest.CancellationReason.OTHER);

        assertThatThrownBy(() -> bookingService.cancelBooking(1L, 1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("additional details");
    }

    @Test
    void cancelBooking_ShouldThrow_WhenAlreadyCheckedIn() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .driverId(1L)
                .status(Booking.BookingStatus.CHECKED_IN)
                .build();

        CancelBookingRequest request = new CancelBookingRequest();
        request.setReason(CancelBookingRequest.CancellationReason.CHANGE_OF_PLANS);

        when(bookingRepository.findById(1L)).thenReturn(java.util.Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L, 1L, request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void extendBooking_ShouldUpdateEndTime_WhenNoConflict() {
        Long bookingId = 1L;
        Long driverId = 10L;
        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .driverId(driverId)
                .status(Booking.BookingStatus.CONFIRMED)
                .bookingType(Booking.BookingType.PRE_BOOKING)
                .endTime(LocalDateTime.now().plusHours(1))
                .startTime(LocalDateTime.now())
                .pricePerHour(10.0)
                .driverEmail("driver@test.com")
                .build();

        ExtendBookingRequest request = new ExtendBookingRequest();
        request.setNewEndTime(LocalDateTime.now().plusHours(3));

        when(bookingRepository.findById(bookingId)).thenReturn(java.util.Optional.of(booking));
        when(bookingRepository.findConflictingBookings(any(), any(), any())).thenReturn(new ArrayList<>());

        BookingResponse response = bookingService.extendBooking(bookingId, driverId, request);

        assertThat(response).isNotNull();
        verify(bookingRepository).save(booking);
    }

    @Test
    void extendBooking_ShouldThrow_WhenNewEndTimeBeforeCurrentEnd() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .driverId(1L)
                .status(Booking.BookingStatus.CONFIRMED)
                .endTime(LocalDateTime.now().plusHours(3))
                .build();

        ExtendBookingRequest request = new ExtendBookingRequest();
        request.setNewEndTime(LocalDateTime.now().plusHours(1));

        when(bookingRepository.findById(1L)).thenReturn(java.util.Optional.of(booking));

        assertThatThrownBy(() -> bookingService.extendBooking(1L, 1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("New end time must be after");
    }

    @Test
    void getBookingsByDriver_ShouldReturnList() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .driverId(1L)
                .bookingType(Booking.BookingType.PRE_BOOKING)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
        when(bookingRepository.findByDriverId(1L)).thenReturn(List.of(booking));

        List<BookingResponse> results = bookingService.getBookingsByDriver(1L);

        assertThat(results).hasSize(1);
    }

    @Test
    void getBookingsByLot_ShouldReturnList() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .driverId(1L)
                .bookingType(Booking.BookingType.WALK_IN)
                .status(Booking.BookingStatus.CHECKED_IN)
                .build();
        when(bookingRepository.findByLotId(1L)).thenReturn(List.of(booking));

        List<BookingResponse> results = bookingService.getBookingsByLot(1L);

        assertThat(results).hasSize(1);
    }

    @Test
    void getAllBookings_ShouldReturnAll() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .driverId(1L)
                .bookingType(Booking.BookingType.PRE_BOOKING)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        List<BookingResponse> results = bookingService.getAllBookings();

        assertThat(results).hasSize(1);
    }

    @Test
    void forceCheckout_ShouldCheckOut_WhenCheckedIn() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .driverId(1L)
                .status(Booking.BookingStatus.CHECKED_IN)
                .bookingType(Booking.BookingType.PRE_BOOKING)
                .checkInTime(LocalDateTime.now().minusHours(1))
                .pricePerHour(10.0)
                .driverEmail("driver@test.com")
                .build();
        when(bookingRepository.findById(1L)).thenReturn(java.util.Optional.of(booking));

        BookingResponse response = bookingService.forceCheckout(1L);

        assertThat(response.getStatus()).isEqualTo(Booking.BookingStatus.CHECKED_OUT.name());
        verify(bookingRepository).save(booking);
    }

    @Test
    void forceCheckout_ShouldThrow_WhenNotCheckedIn() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
        when(bookingRepository.findById(1L)).thenReturn(java.util.Optional.of(booking));

        assertThatThrownBy(() -> bookingService.forceCheckout(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CHECKED_IN");
    }

    @Test
    void deleteBookingsByDriver_ShouldDeleteAll() {
        Booking booking = Booking.builder().bookingId(1L).driverId(1L)
                .bookingType(Booking.BookingType.PRE_BOOKING)
                .status(Booking.BookingStatus.CONFIRMED).build();
        when(bookingRepository.findByDriverId(1L)).thenReturn(List.of(booking));

        bookingService.deleteBookingsByDriver(1L);

        verify(bookingRepository).deleteAll(List.of(booking));
    }

    @Test
    void handlePaymentCompleted_ShouldConfirmBooking_WhenPending() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .driverId(1L)
                .status(Booking.BookingStatus.PENDING)
                .bookingType(Booking.BookingType.PRE_BOOKING)
                .driverEmail("driver@test.com")
                .build();
        when(bookingRepository.findById(1L)).thenReturn(java.util.Optional.of(booking));

        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put("bookingId", 1L);
        event.put("amount", 100.0);

        bookingService.handlePaymentCompleted(event);

        assertThat(booking.getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
        verify(bookingRepository).save(booking);
    }

    @Test
    void handlePaymentCompleted_ShouldSkip_WhenNullBookingId() {
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put("amount", 100.0);

        bookingService.handlePaymentCompleted(event);

        verifyNoInteractions(bookingRepository);
    }
}
