package org.n11bootcamp.inventoryservice.exceptions;

public record FieldErrorResponse(String field,
                                 String message) {

}
