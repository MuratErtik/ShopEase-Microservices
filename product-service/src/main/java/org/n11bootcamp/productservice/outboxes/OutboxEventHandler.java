package org.n11bootcamp.productservice.outboxes;

import org.n11bootcamp.productservice.entities.OutboxEvent;
import org.n11bootcamp.productservice.enums.TargetSystem;

public interface OutboxEventHandler {
    void handle(OutboxEvent event);
    TargetSystem getTargetSystem();
}
