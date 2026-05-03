package org.n11bootcamp.inventoryservice.outboxes;


import org.n11bootcamp.inventoryservice.entities.OutboxEvent;
import org.n11bootcamp.inventoryservice.enums.TargetSystem;

public interface OutboxEventHandler {
    void handle(OutboxEvent event);
    TargetSystem getTargetSystem();
}
