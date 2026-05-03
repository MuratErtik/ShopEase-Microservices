package org.n11bootcamp.userservice.exceptions;

public record FieldErrorResponse(String field,
                                 String message) {

}
