package org.n11bootcamp.orderservice.exceptions;

public record FieldErrorResponse(String field,
                                 String message) {

}
