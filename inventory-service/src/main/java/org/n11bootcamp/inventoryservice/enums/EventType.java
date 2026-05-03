package org.n11bootcamp.inventoryservice.enums;

public enum EventType {
    INVENTORY_CREATED,   // ProductCreatedEvent dinlenince stok kaydı oluştu
    STOCK_UPDATED,       // Seller manuel miktar değiştirdi
    STOCK_RESERVED,      // Saga — OrderCreated geldi, stok rezerve edildi
    STOCK_RELEASED,
    PRODUCT_DELETED,
    STOCK_RESERVATION_FAILED
}
