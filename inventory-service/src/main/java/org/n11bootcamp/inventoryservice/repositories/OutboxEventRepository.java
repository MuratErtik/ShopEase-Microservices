package org.n11bootcamp.inventoryservice.repositories;

import org.n11bootcamp.inventoryservice.entities.OutboxEvent;
import org.n11bootcamp.inventoryservice.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
