package com.parkease.booking.web.resource;

import com.parkease.booking.service.BookingService;
import com.parkease.booking.web.dto.request.*;
import com.parkease.booking.web.dto.response.BookingResponse;
import com.parkease.booking.web.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking lifecycle — create, check-in, check-out, cancel, extend")
public class BookingResource {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Create a new booking — DRIVER only",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Spot not available",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BookingResponse> createBooking(
            @RequestHeader("X-User-Id") Long driverId,
            @Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(driverId, request));
    }

    @PutMapping("/{bookingId}/checkin")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Check in to spot — DRIVER only",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checked in successfully"),
            @ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Cannot check in — invalid status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BookingResponse> checkIn(
            @RequestHeader("X-User-Id") Long driverId,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.checkIn(bookingId, driverId));
    }

    @PutMapping("/{bookingId}/checkout")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Check out of spot — triggers fare calculation",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checked out, fare calculated"),
            @ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Cannot check out — not checked in",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BookingResponse> checkOut(
            @RequestHeader("X-User-Id") Long driverId,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.checkOut(bookingId, driverId));
    }

    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Cancel a booking",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled"),
            @ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Cannot cancel — already checked in",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BookingResponse> cancelBooking(
            @RequestHeader("X-User-Id") Long driverId,
            @PathVariable Long bookingId,
            @Valid @RequestBody CancelBookingRequest request) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId, driverId, request));
    }

    @PutMapping("/{bookingId}/extend")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Extend booking duration",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking extended"),
            @ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Cannot extend — spot not available",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BookingResponse> extendBooking(
            @RequestHeader("X-User-Id") Long driverId,
            @PathVariable Long bookingId,
            @Valid @RequestBody ExtendBookingRequest request) {
        return ResponseEntity.ok(bookingService.extendBooking(bookingId, driverId, request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get all bookings for current driver",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @RequestHeader("X-User-Id") Long driverId) {
        return ResponseEntity.ok(bookingService.getBookingsByDriver(driverId));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking by ID",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<BookingResponse> getBookingById(
            @RequestHeader("X-User-Id") Long driverId,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId, driverId));
    }

    @GetMapping("/manager/lot/{lotId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get all bookings for a lot — MANAGER only",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<BookingResponse>> getBookingsForLot(@PathVariable Long lotId) {
        return ResponseEntity.ok(bookingService.getBookingsByLot(lotId));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all bookings platform-wide — ADMIN only",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @PutMapping("/admin/{bookingId}/force-checkout")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force checkout a booking — ADMIN only",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<BookingResponse> forceCheckout(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.forceCheckout(bookingId));
    }
}
