package com.parkease.payment.web.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RevenueReportResponse {
    private Long lotId;
    private Double totalRevenue;
    private Long totalTransactions;
    private String fromDate;
    private String toDate;
    private String currency;
}
