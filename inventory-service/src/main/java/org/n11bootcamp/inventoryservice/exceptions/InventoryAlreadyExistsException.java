package org.n11bootcamp.inventoryservice.exceptions;

public class InventoryAlreadyExistsException extends RuntimeException {
    public InventoryAlreadyExistsException(String message) {
        super(message);
    }
}
