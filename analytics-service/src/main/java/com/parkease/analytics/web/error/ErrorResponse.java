package com.parkease.analytics.web.error;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private Map<String, String> fieldErrors;
}
