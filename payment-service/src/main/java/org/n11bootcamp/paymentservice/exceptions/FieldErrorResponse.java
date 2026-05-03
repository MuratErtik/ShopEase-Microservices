package org.n11bootcamp.paymentservice.exceptions;

public record FieldErrorResponse(String field,
                                 String message) {

}
