package org.n11bootcamp.inventoryservice.exceptions;

public class SellerNotAuthorizedThisProductException extends RuntimeException {
    public SellerNotAuthorizedThisProductException(String message) {
        super(message);
    }
}
