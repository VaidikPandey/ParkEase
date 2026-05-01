package com.parkease.booking.service.impl;

import com.parkease.booking.config.RabbitMQConfig;
import com.parkease.booking.domain.entity.Booking;
import com.parkease.booking.domain.entity.BookingEvent;
import com.parkease.booking.repository.BookingRepository;
import com.parkease.booking.service.BookingService;
import com.parkease.booking.web.dto.request.*;
import com.parkease.booking.web.dto.response.BookingResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RedissonClient redissonClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${booking.lock.timeout-seconds}")
    private long lockTimeoutSeconds;

    @Value("${booking.grace-period-minutes}")
    private int gracePeriodMinutes;

    @Value("${booking.cancellation-window-minutes}")
    private int cancellationWindowMinutes;

    // ── Create Booking

    @Override
    public BookingResponse createBooking(Long driverId, CreateBookingRequest request) {

        // validate time window
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // Redisson distributed lock — key per spot
        // prevents two drivers booking same spot simultaneously
        String lockKey = "booking:lock:spot:" + request.getSpotId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // try to acquire lock — wait max 10 seconds
            boolean acquired = lock.tryLock(lockTimeoutSeconds, lockTimeoutSeconds, TimeUnit.SECONDS);

            if (!acquired) {
                throw new IllegalStateException(
                        "Spot is being booked by another user. Please try again."
                );
            }

            // check for conflicting bookings
            List<Booking> conflicts = bookingRepository.findConflictingBookings(
                    request.getSpotId(),
                    request.getStartTime(),
                    request.getEndTime()
            );

            if (!conflicts.isEmpty()) {
                throw new IllegalStateException(
                        "Spot is not available for the selected time window"
                );
            }

            // create booking
            Booking booking = Booking.builder()
                    .driverId(driverId)
                    .spotId(request.getSpotId())
                    .lotId(request.getLotId())
                    .spotNumber(request.getSpotNumber())
                    .bookingType(request.getBookingType())
                    .status(Booking.BookingStatus.PENDING)
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .pricePerHour(request.getPricePerHour())
                    .vehiclePlate(request.getVehiclePlate())
                    .driverEmail(request.getDriverEmail())
                    .build();

            bookingRepository.save(booking);
            log.info("Booking created: id={} driver={} spot={}",
                    booking.getBookingId(), driverId, request.getSpotId());

            // publish event to RabbitMQ
            publishEvent(booking, request.getDriverEmail(),
                    RabbitMQConfig.BOOKING_PENDING_KEY);

            return BookingResponse.from(booking);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Booking interrupted. Please try again.");
        } finally {
            // always release lock
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ── Check In

    @Override
    public BookingResponse checkIn(Long bookingId, Long driverId) {
        Booking booking = findBookingById(bookingId);
        validateDriverOwnership(booking, driverId);

        if (booking.getStatus() != Booking.BookingStatus.PENDING &&
                booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Cannot check in. Booking status is: " + booking.getStatus()
            );
        }

        booking.setStatus(Booking.BookingStatus.CHECKED_IN);
        booking.setCheckInTime(LocalDateTime.now());
        bookingRepository.save(booking);

        log.info("Check-in: bookingId={} driverId={}", bookingId, driverId);

        publishEvent(booking, booking.getDriverEmail(), RabbitMQConfig.BOOKING_CHECKIN_KEY);

        return BookingResponse.from(booking);
    }

    // ── Check Out

    @Override
    public BookingResponse checkOut(Long bookingId, Long driverId) {
        Booking booking = findBookingById(bookingId);
        validateDriverOwnership(booking, driverId);

        if (booking.getStatus() != Booking.BookingStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "Cannot check out. Booking status is: " + booking.getStatus()
            );
        }

        LocalDateTime checkOutTime = LocalDateTime.now();
        booking.setCheckOutTime(checkOutTime);
        booking.setStatus(Booking.BookingStatus.CHECKED_OUT);

        // charge based on actual time parked (early checkout = lower fare)
        double fare = calculateFare(booking.getCheckInTime(), checkOutTime, booking.getPricePerHour());
        booking.setTotalFare(fare);

        bookingRepository.save(booking);

        log.info("Check-out: bookingId={} fare={}", bookingId, fare);

        publishEvent(booking, booking.getDriverEmail(), RabbitMQConfig.BOOKING_CHECKOUT_KEY);

        return BookingResponse.from(booking);
    }

    // ── Cancel Booking

    @Override
    public BookingResponse cancelBooking(
            Long bookingId, Long driverId, CancelBookingRequest request
    ) {
        // validate OTHER reason
        if (!request.isValid()) {
            throw new IllegalArgumentException(
                    "Please provide additional details when selecting OTHER as reason"
            );
        }

        Booking booking = findBookingById(bookingId);
        validateDriverOwnership(booking, driverId);

        if (booking.getStatus() == Booking.BookingStatus.CHECKED_IN ||
                booking.getStatus() == Booking.BookingStatus.CHECKED_OUT) {
            throw new IllegalStateException(
                    "Cannot cancel a booking that is already checked in or completed"
            );
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        // build cancellation reason string
        String reason = request.getReason().name();
        if (request.getAdditionalNote() != null && !request.getAdditionalNote().isBlank()) {
            reason += " — " + request.getAdditionalNote();
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        bookingRepository.save(booking);

        log.info("Booking cancelled: id={} reason={}", bookingId, reason);

        publishEvent(booking, booking.getDriverEmail(), RabbitMQConfig.BOOKING_CANCELLED_KEY);

        return BookingResponse.from(booking);
    }

    // ── Extend Booking

    @Override
    public BookingResponse extendBooking(
            Long bookingId, Long driverId, ExtendBookingRequest request
    ) {
        Booking booking = findBookingById(bookingId);
        validateDriverOwnership(booking, driverId);

        if (booking.getStatus() != Booking.BookingStatus.CHECKED_IN &&
                booking.getStatus() != Booking.BookingStatus.PENDING &&
                booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Cannot extend booking with status: " + booking.getStatus()
            );
        }

        if (!request.getNewEndTime().isAfter(booking.getEndTime())) {
            throw new IllegalArgumentException(
                    "New end time must be after current end time"
            );
        }

        // check no conflicts for extended time
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                booking.getSpotId(),
                booking.getEndTime(),
                request.getNewEndTime()
        );

        // filter out current booking from conflicts
        conflicts = conflicts.stream()
                .filter(b -> !b.getBookingId().equals(bookingId))
                .toList();

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException(
                    "Spot is not available for the extended time window"
            );
        }

        booking.setEndTime(request.getNewEndTime());

        LocalDateTime fareStart = booking.getCheckInTime() != null
                ? booking.getCheckInTime()
                : booking.getStartTime();
        double updatedFare = calculateFare(fareStart, request.getNewEndTime(), booking.getPricePerHour());
        booking.setTotalFare(updatedFare);

        bookingRepository.save(booking);

        log.info("Booking extended: id={} newEndTime={} updatedFare={}", bookingId, request.getNewEndTime(), updatedFare);

        return BookingResponse.from(booking);
    }

    // ── Get Booking By ID

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId, Long driverId) {
        Booking booking = findBookingById(bookingId);
        validateDriverOwnership(booking, driverId);
        return BookingResponse.from(booking);
    }

    // ── Get By Driver

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByDriver(Long driverId) {
        return bookingRepository.findByDriverId(driverId)
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    // ── Get By Lot

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByLot(Long lotId) {
        return bookingRepository.findByLotId(lotId)
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    // ── Get All

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    // ── Force Checkout (Admin)

    @Override
    public BookingResponse forceCheckout(Long bookingId) {
        Booking booking = findBookingById(bookingId);

        if (booking.getStatus() != Booking.BookingStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "Can only force checkout a CHECKED_IN booking"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        booking.setCheckOutTime(now);
        booking.setStatus(Booking.BookingStatus.CHECKED_OUT);

        double fare = calculateFare(booking.getCheckInTime(), now, booking.getPricePerHour());
        booking.setTotalFare(fare);

        bookingRepository.save(booking);
        log.info("Force checkout: bookingId={} fare={}", bookingId, fare);

        publishEvent(booking, booking.getDriverEmail(), RabbitMQConfig.BOOKING_CHECKOUT_KEY);

        return BookingResponse.from(booking);
    }

    // ── Auto Cancel Expired Bookings (Scheduled)

    @Override
    @Scheduled(fixedDelay = 60000)   // runs every 60 seconds
    public void autoCancelExpiredBookings() {
        LocalDateTime cutoffTime = LocalDateTime.now()
                .minusMinutes(gracePeriodMinutes);

        List<Booking> expired = bookingRepository.findExpiredBookings(cutoffTime);

        for (Booking booking : expired) {
            booking.setStatus(Booking.BookingStatus.EXPIRED);
            booking.setCancellationReason("Auto-cancelled: no check-in within grace period");
            bookingRepository.save(booking);

            publishEvent(booking, booking.getDriverEmail(), RabbitMQConfig.BOOKING_EXPIRY_KEY);

            log.info("Auto-cancelled expired booking: id={}", booking.getBookingId());
        }

        if (!expired.isEmpty()) {
            log.info("Auto-cancelled {} expired bookings", expired.size());
        }
    }

    // ── Expiry Reminder (15 min before end time)

    @Scheduled(fixedDelay = 60000)
    public void sendExpiryReminders() {
        LocalDateTime now          = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(15);

        List<Booking> endingSoon = bookingRepository.findBookingsEndingSoon(now, reminderTime);

        for (Booking booking : endingSoon) {
            publishEvent(booking, booking.getDriverEmail(), RabbitMQConfig.BOOKING_REMINDER_KEY);
            booking.setReminderSent(true);
            bookingRepository.save(booking);
            log.info("Sent expiry reminder for booking: id={}", booking.getBookingId());
        }
    }

    // ── Admin Cascade Delete

    @Override
    public void deleteBookingsByDriver(Long driverId) {
        List<Booking> bookings = bookingRepository.findByDriverId(driverId);
        bookingRepository.deleteAll(bookings);
        log.info("Deleted {} bookings for driverId={}", bookings.size(), driverId);
    }

    @Override
    public void deleteBookingsByLot(Long lotId) {
        List<Booking> bookings = bookingRepository.findByLotId(lotId);
        bookingRepository.deleteAll(bookings);
        log.info("Deleted {} bookings for lotId={}", bookings.size(), lotId);
    }

    // ── Payment Completed Event

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_EVENTS_QUEUE)
    public void handlePaymentCompleted(Map<String, Object> event) {
        Long bookingId = toLong(event.get("bookingId"));
        Double amount = toDouble(event.get("amount"));

        if (bookingId == null) {
            log.warn("Ignoring payment.completed event without bookingId");
            return;
        }

        Booking booking = findBookingById(bookingId);

        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
            log.info("Booking already confirmed: id={}", bookingId);
            return;
        }

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            log.warn("Ignoring payment.completed for bookingId={} with status={}",
                    bookingId, booking.getStatus());
            return;
        }

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        if (amount != null) {
            booking.setTotalFare(amount);
        }
        bookingRepository.save(booking);

        log.info("Booking confirmed after payment: bookingId={} amount={}", bookingId, amount);
        publishEvent(booking, booking.getDriverEmail(), RabbitMQConfig.BOOKING_CONFIRMED_KEY);
    }

    // ── Private Helpers

    private Booking findBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Booking not found: id=" + bookingId));
    }

    private void validateDriverOwnership(Booking booking, Long driverId) {
        if (!booking.getDriverId().equals(driverId)) {
            throw new IllegalArgumentException("You do not own this booking");
        }
    }

    private double calculateFare(
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            double pricePerHour
    ) {
        long minutes = ChronoUnit.MINUTES.between(checkIn, checkOut);
        double hours = Math.max(1.0, minutes / 60.0);  // minimum 1 hour charge
        double fare = Math.ceil(hours * pricePerHour * 100) / 100.0; // round up to 2 decimals
        return fare;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void publishEvent(Booking booking, String driverEmail, String routingKey) {
        try {
            BookingEvent event = BookingEvent.builder()
                    .bookingId(booking.getBookingId())
                    .driverId(booking.getDriverId())
                    .spotId(booking.getSpotId())
                    .lotId(booking.getLotId())
                    .spotNumber(booking.getSpotNumber())
                    .driverEmail(driverEmail)
                    .vehiclePlate(booking.getVehiclePlate())
                    .eventType(routingKey)
                    .bookingType(booking.getBookingType() != null ? booking.getBookingType().name() : null)
                    .startTime(booking.getStartTime())
                    .endTime(booking.getEndTime())
                    .pricePerHour(booking.getPricePerHour())
                    .checkInTime(booking.getCheckInTime())
                    .checkOutTime(booking.getCheckOutTime())
                    .totalFare(booking.getTotalFare())
                    .occurredAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BOOKING_EXCHANGE,
                    routingKey,
                    event
            );

            log.debug("Event published: {} for bookingId={}",
                    routingKey, booking.getBookingId());

        } catch (Exception e) {
            // don't fail the main operation if event publishing fails
            log.error("Failed to publish event {} for bookingId={}: {}",
                    routingKey, booking.getBookingId(), e.getMessage());
        }
    }
}
