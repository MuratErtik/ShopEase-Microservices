package org.n11bootcamp.cartservice.exceptions;

public record FieldErrorResponse(String field,
                                 String message) {

}
