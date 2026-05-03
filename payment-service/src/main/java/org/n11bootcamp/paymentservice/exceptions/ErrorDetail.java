package org.n11bootcamp.paymentservice.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetail {

    private int status;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, String> validationErrors;


    private String error;

    private String details;

}
