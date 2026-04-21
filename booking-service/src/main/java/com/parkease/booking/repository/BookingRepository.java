package com.parkease.booking.repository;

import com.parkease.booking.domain.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByDriverId(Long driverId);

    List<Booking> findByLotId(Long lotId);

    List<Booking> findBySpotId(Long spotId);

    List<Booking> findByDriverIdAndStatus(Long driverId, Booking.BookingStatus status);

    List<Booking> findByLotIdAndStatus(Long lotId, Booking.BookingStatus status);

    Optional<Booking> findBySpotIdAndStatus(Long spotId, Booking.BookingStatus status);

    // find all active bookings for a spot (PENDING, CONFIRMED, CHECKED_IN)
    @Query("""
        SELECT b FROM Booking b
        WHERE b.spotId = :spotId
        AND b.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN')
        AND b.startTime < :endTime
        AND b.endTime > :startTime
        """)
    List<Booking> findConflictingBookings(
            @Param("spotId") Long spotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // find expired bookings — PENDING with no check-in after grace period
    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = 'PENDING'
        AND b.startTime < :cutoffTime
        """)
    List<Booking> findExpiredBookings(@Param("cutoffTime") LocalDateTime cutoffTime);

    // find bookings ending soon — for expiry reminder notifications
    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = 'CHECKED_IN'
        AND b.endTime BETWEEN :now AND :reminderTime
        """)
    List<Booking> findBookingsEndingSoon(
            @Param("now") LocalDateTime now,
            @Param("reminderTime") LocalDateTime reminderTime
    );
}