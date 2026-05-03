package org.n11bootcamp.productservice.exceptions;

public record FieldErrorResponse(String field,
                                 String message) {

}
