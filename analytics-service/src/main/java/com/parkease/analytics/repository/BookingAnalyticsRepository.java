package com.parkease.analytics.repository;

import com.parkease.analytics.domain.entity.BookingAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingAnalyticsRepository extends JpaRepository<BookingAnalytics, Long> {

    Optional<BookingAnalytics> findByBookingId(Long bookingId);

    List<BookingAnalytics> findByLotIdAndStatusIn(
            Long lotId, List<BookingAnalytics.BookingStatus> statuses);

    long countByLotIdAndStatus(Long lotId, BookingAnalytics.BookingStatus status);

    @Query("""
            SELECT ba FROM BookingAnalytics ba
            WHERE ba.lotId = :lotId
              AND ba.status = 'COMPLETED'
              AND ba.checkinAt >= :from
              AND ba.checkoutAt <= :to
            """)
    List<BookingAnalytics> findCompletedInRange(
            @Param("lotId") Long lotId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT ba.bookingType, COUNT(ba)
            FROM BookingAnalytics ba
            WHERE ba.lotId = :lotId
              AND ba.confirmedAt >= :from
              AND ba.confirmedAt <= :to
            GROUP BY ba.bookingType
            """)
    List<Object[]> countByBookingType(
            @Param("lotId") Long lotId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT EXTRACT(HOUR FROM confirmed_at)::int AS hour, COUNT(*) AS cnt
            FROM booking_analytics
            WHERE lot_id = :lotId
              AND confirmed_at >= :from
              AND confirmed_at <= :to
            GROUP BY hour
            ORDER BY hour
            """, nativeQuery = true)
    List<Object[]> countByHour(
            @Param("lotId") Long lotId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT ba FROM BookingAnalytics ba
            WHERE ba.driverId = :driverId
              AND ba.confirmedAt >= :from
              AND ba.confirmedAt <= :to
            ORDER BY ba.confirmedAt DESC
            """)
    List<BookingAnalytics> findByDriverIdInRange(
            @Param("driverId") Long driverId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT ba.bookingType, COUNT(ba)
            FROM BookingAnalytics ba
            WHERE ba.driverId = :driverId
              AND ba.confirmedAt >= :from
              AND ba.confirmedAt <= :to
            GROUP BY ba.bookingType
            """)
    List<Object[]> countByBookingTypeForDriver(
            @Param("driverId") Long driverId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
