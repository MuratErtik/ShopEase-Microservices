package org.n11bootcamp.inventoryservice.exceptions;

public class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(String message) {
        super(message);
    }
}
