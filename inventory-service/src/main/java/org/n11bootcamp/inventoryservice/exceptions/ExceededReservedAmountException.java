package org.n11bootcamp.inventoryservice.exceptions;

public class ExceededReservedAmountException extends RuntimeException {
    public ExceededReservedAmountException(String message) {
        super(message);
    }
}
