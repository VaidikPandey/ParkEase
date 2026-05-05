package com.parkease.payment.web.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.payment.domain.entity.Payment;
import com.parkease.payment.security.HeaderAuthFilter;
import com.parkease.payment.service.PaymentService;
import com.parkease.payment.web.dto.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentResource.class)
@Import({PaymentResourceTest.TestSecurity.class, HeaderAuthFilter.class})
class PaymentResourceTest {

    @TestConfiguration
    static class TestSecurity {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http, HeaderAuthFilter headerAuthFilter) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrder_ShouldReturn200() throws Exception {
        RazorpayOrderRequest request = new RazorpayOrderRequest();
        request.setBookingId(1L);
        request.setAmount(100.0);

        RazorpayOrderResponse response = RazorpayOrderResponse.builder()
                .orderId("order_123")
                .keyId("key_abc")
                .build();

        when(paymentService.createOrder(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/create-order")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "DRIVER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order_123"));
    }

    @Test
    void processPayment_ShouldReturn201_WhenSuccessful() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(1L);
        request.setLotId(1L);
        request.setAmount(100.0);
        request.setMode(Payment.PaymentMode.UPI);

        PaymentResponse paymentResponse = PaymentResponse.builder()
                .bookingId(1L)
                .status(Payment.PaymentStatus.PAID)
                .build();

        when(paymentService.processPayment(any(), anyLong(), eq(false))).thenReturn(paymentResponse);

        mockMvc.perform(post("/api/v1/payments")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "DRIVER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void processPayment_ShouldReturn400_WhenInvalidInput() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(100.0);

        mockMvc.perform(post("/api/v1/payments")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "DRIVER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processPayment_ShouldReturn500_WhenServiceFails() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(1L);
        request.setLotId(1L);
        request.setAmount(100.0);
        request.setMode(Payment.PaymentMode.UPI);

        when(paymentService.processPayment(any(), anyLong(), anyBoolean()))
                .thenThrow(new RuntimeException("Simulated payment gateway error"));

        mockMvc.perform(post("/api/v1/payments")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "DRIVER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void refundPayment_ShouldReturn200_WhenSuccessful() throws Exception {
        RefundRequest request = new RefundRequest();
        request.setBookingId(1L);
        request.setReason("Customer request");

        PaymentResponse refundResponse = PaymentResponse.builder()
                .bookingId(1L)
                .status(Payment.PaymentStatus.REFUNDED)
                .build();

        when(paymentService.refundPayment(any(), anyLong(), anyBoolean())).thenReturn(refundResponse);

        mockMvc.perform(post("/api/v1/payments/refund")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "DRIVER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void refundPayment_ShouldReturn404_WhenBookingNotFound() throws Exception {
        RefundRequest request = new RefundRequest();
        request.setBookingId(999L);

        when(paymentService.refundPayment(any(), anyLong(), anyBoolean()))
                .thenThrow(new EntityNotFoundException("No payment found for bookingId: 999"));

        mockMvc.perform(post("/api/v1/payments/refund")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "DRIVER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void refundPayment_ShouldReturn403_WhenNotOwner() throws Exception {
        RefundRequest request = new RefundRequest();
        request.setBookingId(1L);

        when(paymentService.refundPayment(any(), anyLong(), anyBoolean()))
                .thenThrow(new AccessDeniedException("You can only access your own payments"));

        mockMvc.perform(post("/api/v1/payments/refund")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "DRIVER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByBooking_ShouldReturn200_WhenAdmin() throws Exception {
        when(paymentService.getByBookingId(anyLong(), anyLong(), eq(true))).thenReturn(PaymentResponse.builder().build());

        mockMvc.perform(get("/api/v1/payments/booking/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getByBooking_ShouldReturn200_WhenOwner() throws Exception {
        when(paymentService.getByBookingId(anyLong(), anyLong(), eq(false))).thenReturn(PaymentResponse.builder().build());

        mockMvc.perform(get("/api/v1/payments/booking/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "DRIVER"))
                .andExpect(status().isOk());
    }

    @Test
    void getByBooking_ShouldReturn403_WhenNotOwner() throws Exception {
        when(paymentService.getByBookingId(anyLong(), anyLong(), eq(false)))
                .thenThrow(new AccessDeniedException("You can only access your own payments"));

        mockMvc.perform(get("/api/v1/payments/booking/1")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "DRIVER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getStatus_ShouldReturn200_WhenOwner() throws Exception {
        when(paymentService.getPaymentStatus(anyLong(), anyLong(), eq(false))).thenReturn(PaymentResponse.builder().build());

        mockMvc.perform(get("/api/v1/payments/1/status")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "DRIVER"))
                .andExpect(status().isOk());
    }

    @Test
    void getStatus_ShouldReturn403_WhenNotOwner() throws Exception {
        when(paymentService.getPaymentStatus(anyLong(), anyLong(), eq(false)))
                .thenThrow(new AccessDeniedException("You can only access your own payments"));

        mockMvc.perform(get("/api/v1/payments/1/status")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "DRIVER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByUser_ShouldReturn200_WhenAdmin() throws Exception {
        when(paymentService.getByUserId(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/payments/user/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getByUser_ShouldReturn200_WhenSameUser() throws Exception {
        when(paymentService.getByUserId(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/payments/user/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "DRIVER"))
                .andExpect(status().isOk());
    }

    @Test
    void getByUser_ShouldReturn403_WhenOtherUser() throws Exception {
        mockMvc.perform(get("/api/v1/payments/user/2")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "DRIVER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadReceipt_ShouldReturn200_WhenOwner() throws Exception {
        when(paymentService.generateReceipt(anyLong(), anyLong(), eq(false))).thenReturn(new byte[0]);

        mockMvc.perform(get("/api/v1/payments/booking/1/receipt")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "DRIVER"))
                .andExpect(status().isOk());
    }

    @Test
    void downloadReceipt_ShouldReturn403_WhenNotOwner() throws Exception {
        when(paymentService.generateReceipt(anyLong(), anyLong(), eq(false)))
                .thenThrow(new AccessDeniedException("You can only access your own payments"));

        mockMvc.perform(get("/api/v1/payments/booking/1/receipt")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "DRIVER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLotRevenue_ShouldReturn200_WhenManager() throws Exception {
        when(paymentService.getRevenueByLot(anyLong(), any(), any())).thenReturn(RevenueReportResponse.builder().build());

        mockMvc.perform(get("/api/v1/payments/admin/revenue/lot/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "MANAGER"))
                .andExpect(status().isOk());
    }

    @Test
    void getPlatformRevenue_ShouldReturn200_WhenAdmin() throws Exception {
        when(paymentService.getTotalPlatformRevenue(any(), any())).thenReturn(1000.0);

        mockMvc.perform(get("/api/v1/payments/admin/revenue/platform")
                        .param("from", "2025-01-01T00:00:00")
                        .param("to", "2025-12-31T23:59:59")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllTimeRevenue_ShouldReturn200_WhenAdmin() throws Exception {
        when(paymentService.getAllTimePlatformRevenue()).thenReturn(5000.0);

        mockMvc.perform(get("/api/v1/payments/admin/revenue/platform/all")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }
}
