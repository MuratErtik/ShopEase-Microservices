package org.n11bootcamp.orderservice.enums;

public enum EventType {
    ORDER_CREATED,
    ORDER_CONFIRMED,
    ORDER_CANCELLED,
    STOCK_RELEASED,  // payment fails, stock release
    PAYMENT_REQUESTED
}
