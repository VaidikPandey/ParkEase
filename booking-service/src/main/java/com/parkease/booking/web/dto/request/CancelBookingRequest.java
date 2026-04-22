package com.parkease.booking.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CancelBookingRequest {

    @NotNull(message = "Cancellation reason is required")
    private CancellationReason reason;

    // required only when reason is OTHER
    private String additionalNote;

    public enum CancellationReason {
        CHANGE_OF_PLANS,
        FOUND_BETTER_SPOT,
        VEHICLE_BREAKDOWN,
        EMERGENCY,
        WRONG_BOOKING,
        TOOK_TOO_LONG,
        OTHER
    }

    // validates that additionalNote is provided when reason is OTHER
    public boolean isValid() {
        if (reason == CancellationReason.OTHER) {
            return additionalNote != null && !additionalNote.isBlank();
        }
        return true;
    }
}