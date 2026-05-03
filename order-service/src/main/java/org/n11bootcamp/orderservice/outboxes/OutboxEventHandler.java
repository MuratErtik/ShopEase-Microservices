package org.n11bootcamp.orderservice.outboxes;

import org.n11bootcamp.orderservice.entities.OutboxEvent;
import org.n11bootcamp.orderservice.enums.TargetSystem;

public interface OutboxEventHandler {
    void handle(OutboxEvent event);
    TargetSystem getTargetSystem();
}
