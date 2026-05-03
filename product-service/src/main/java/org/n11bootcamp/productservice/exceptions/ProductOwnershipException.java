package org.n11bootcamp.productservice.exceptions;

public class ProductOwnershipException extends RuntimeException {
    public ProductOwnershipException(String message) {
        super(message);
    }
}
