package com.parkease.booking.service;

import com.parkease.booking.web.dto.request.*;
import com.parkease.booking.web.dto.response.BookingResponse;

import java.util.List;

public interface BookingService {
    BookingResponse createBooking(Long driverId, CreateBookingRequest request);
    BookingResponse checkIn(Long bookingId, Long driverId);
    BookingResponse checkOut(Long bookingId, Long driverId);
    BookingResponse cancelBooking(Long bookingId, Long driverId, CancelBookingRequest request);
    BookingResponse extendBooking(Long bookingId, Long driverId, ExtendBookingRequest request);
    BookingResponse getBookingById(Long bookingId, Long driverId);
    List<BookingResponse> getBookingsByDriver(Long driverId);
    List<BookingResponse> getBookingsByLot(Long lotId);
    List<BookingResponse> getAllBookings();
    BookingResponse forceCheckout(Long bookingId);
    void autoCancelExpiredBookings();
    void deleteBookingsByDriver(Long driverId);
    void deleteBookingsByLot(Long lotId);
}