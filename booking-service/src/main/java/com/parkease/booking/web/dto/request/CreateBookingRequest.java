package com.parkease.booking.web.dto.request;

import com.parkease.booking.domain.entity.Booking;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateBookingRequest {

    @NotNull(message = "Spot ID is required")
    private Long spotId;

    @NotNull(message = "Lot ID is required")
    private Long lotId;

    @NotBlank(message = "Spot number is required")
    private String spotNumber;

    @NotNull(message = "Booking type is required")
    private Booking.BookingType bookingType;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;

    @NotNull(message = "Price per hour is required")
    @Positive(message = "Price must be positive")
    private Double pricePerHour;

    @NotBlank(message = "Vehicle plate is required")
    private String vehiclePlate;

    @NotBlank(message = "Driver email is required")
    @Email
    private String driverEmail;
}