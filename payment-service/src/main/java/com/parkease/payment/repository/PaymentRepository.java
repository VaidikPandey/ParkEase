package com.parkease.payment.repository;

import com.parkease.payment.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByPaidAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.lotId = :lotId AND p.status = 'PAID'")
    Double sumAmountByLotId(@Param("lotId") Long lotId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.lotId = :lotId AND p.status = 'PAID' " +
           "AND p.paidAt BETWEEN :from AND :to")
    Double sumAmountByLotIdAndDateRange(@Param("lotId") Long lotId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.lotId = :lotId AND p.status = 'PAID'")
    Long countByLotId(@Param("lotId") Long lotId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.status = 'PAID' AND p.paidAt BETWEEN :from AND :to")
    Double sumTotalRevenueBetween(@Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'PAID'")
    Double sumTotalRevenue();
}
