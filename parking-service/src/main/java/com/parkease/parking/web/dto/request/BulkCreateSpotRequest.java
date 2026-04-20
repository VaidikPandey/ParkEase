package com.parkease.parking.web.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class BulkCreateSpotRequest {

    @NotNull
    @Size(min = 1, message = "At least one spot is required")
    private List<CreateSpotRequest> spots;
}
