package com.parkease.payment.web.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RazorpayOrderResponse {
    private String orderId;
    private Integer amount;
    private String currency;
    private String keyId;
}
