package org.n11bootcamp.orderservice.exceptions;





import java.time.LocalDateTime;
import java.util.List;

public record ValidationErrorResponse(LocalDateTime timestamp,
                                      int status,
                                      String error,
                                      String path,
                                      List<FieldErrorResponse> fields) {
}
