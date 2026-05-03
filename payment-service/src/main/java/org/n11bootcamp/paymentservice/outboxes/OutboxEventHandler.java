package org.n11bootcamp.paymentservice.outboxes;



import org.n11bootcamp.paymentservice.entities.OutboxEvent;
import org.n11bootcamp.paymentservice.enums.TargetSystem;

public interface OutboxEventHandler {
    void handle(OutboxEvent event);
    TargetSystem getTargetSystem();
}