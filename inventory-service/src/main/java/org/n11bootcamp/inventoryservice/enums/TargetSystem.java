package org.n11bootcamp.inventoryservice.enums;

public enum TargetSystem {

    ORDER_SERVICE,        // StockReserved / StockReleased → Saga devam etsin
    NOTIFICATION_SERVICE,
    CART_SERVICE
}
