package com.parkease.analytics.web.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ActiveBookingsResponse {
    private Long lotId;
    private int confirmedPending;       // status=CONFIRMED, awaiting check-in
    private int currentlyParked;        // status=ACTIVE, checked in
    private int total;
    private List<ActiveBookingDetail> activeBookings;
}
