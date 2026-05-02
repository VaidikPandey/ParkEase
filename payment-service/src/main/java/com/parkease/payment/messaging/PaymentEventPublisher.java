package com.parkease.payment.messaging;

import com.parkease.payment.domain.entity.Payment;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import com.parkease.payment.config.RabbitMQConfig;


@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentEvent(Payment payment) {
        Map<String, Object> event = buildBaseEvent(payment);
        event.put("eventType", "payment.completed");
        event.put("transactionId", payment.getTransactionId());
        event.put("mode", payment.getMode().name());
        event.put("currency", payment.getCurrency());
        publish(event, "payment.completed", payment.getBookingId());
    }

    public void publishRefundEvent(Payment payment) {
        Map<String, Object> event = buildBaseEvent(payment);
        event.put("eventType", "payment.refunded");
        event.put("refundedAt", payment.getRefundedAt() != null ? payment.getRefundedAt().toString() : null);
        publish(event, "payment.refunded", payment.getBookingId());
    }

    private Map<String, Object> buildBaseEvent(Payment payment) {
        Map<String, Object> event = new HashMap<>();
        event.put("paymentId", payment.getPaymentId());
        event.put("bookingId", payment.getBookingId());
        event.put("userId", payment.getUserId());
        event.put("lotId", payment.getLotId());
        event.put("amount", payment.getAmount());
        return event;
    }

    private void publish(Map<String, Object> event, String routingKey, Long bookingId) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.BOOKING_EXCHANGE, routingKey, event);
            log.info("Published event type={} for bookingId={}", routingKey, bookingId);
        } catch (Exception e) {
            log.error("Failed to publish event for bookingId={}: {}", bookingId, e.getMessage());
        }
    }
}
