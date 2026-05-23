package com.ecommerce.userservice.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standard error response")
public class ErrorResponse {

    @Schema(description = "Timestamp of the error")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code")
    private int status;

    @Schema(description = "Error type")
    private String error;

    @Schema(description = "Error message")
    private String message;

    @Schema(description = "Field-level validation errors")
    private Map<String, String> fieldErrors;
}
