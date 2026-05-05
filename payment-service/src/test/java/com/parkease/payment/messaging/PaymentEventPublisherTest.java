package com.parkease.payment.messaging;

import com.parkease.payment.domain.entity.Payment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentEventPublisher publisher;

    private Payment samplePayment() {
        Payment p = new Payment();
        p.setPaymentId(1L);
        p.setBookingId(10L);
        p.setUserId(5L);
        p.setLotId(2L);
        p.setAmount(100.0);
        p.setTransactionId("TXN123");
        p.setMode(Payment.PaymentMode.CARD);
        p.setCurrency("INR");
        return p;
    }

    @Test
    void publishPaymentEvent_ShouldSendToExchange() {
        publisher.publishPaymentEvent(samplePayment());

        verify(rabbitTemplate).convertAndSend(anyString(), eq("payment.completed"), any(Map.class));
    }

    @Test
    void publishRefundEvent_ShouldSendToExchange() {
        Payment p = samplePayment();
        p.setRefundedAt(LocalDateTime.now());

        publisher.publishRefundEvent(p);

        verify(rabbitTemplate).convertAndSend(anyString(), eq("payment.refunded"), any(Map.class));
    }

    @Test
    void publishPaymentEvent_ShouldNotThrow_WhenRabbitFails() {
        doThrow(new RuntimeException("connection refused"))
            .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Map.class));

        publisher.publishPaymentEvent(samplePayment());
    }
}
