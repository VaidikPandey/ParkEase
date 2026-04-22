package com.parkease.booking.web.dto.response;

import com.parkease.booking.domain.entity.Booking;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponse {

    private Long bookingId;
    private Long driverId;
    private Long spotId;
    private Long lotId;
    private String spotNumber;
    private String bookingType;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Double pricePerHour;
    private String pricePerHourFormatted;
    private Double totalFare;
    private String totalFareFormatted;
    private String currency;
    private String vehiclePlate;
    private String cancellationReason;
    private LocalDateTime createdAt;

    public static BookingResponse from(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .driverId(booking.getDriverId())
                .spotId(booking.getSpotId())
                .lotId(booking.getLotId())
                .spotNumber(booking.getSpotNumber())
                .bookingType(booking.getBookingType().name())
                .status(booking.getStatus().name())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .checkInTime(booking.getCheckInTime())
                .checkOutTime(booking.getCheckOutTime())
                .pricePerHour(booking.getPricePerHour())
                .pricePerHourFormatted(booking.getPricePerHour() != null
                        ? "₹" + String.format("%.2f", booking.getPricePerHour())
                        : null)
                .totalFare(booking.getTotalFare())
                .totalFareFormatted(booking.getTotalFare() != null
                        ? "₹" + String.format("%.2f", booking.getTotalFare())
                        : null)
                .currency("INR")
                .vehiclePlate(booking.getVehiclePlate())
                .cancellationReason(booking.getCancellationReason())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}