package org.n11bootcamp.paymentservice.exceptions;

public class DuplicateOrderException extends RuntimeException {
    public DuplicateOrderException(String message) {
        super(message);
    }
}
