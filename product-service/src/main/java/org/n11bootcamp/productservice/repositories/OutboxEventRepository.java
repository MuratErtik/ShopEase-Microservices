package org.n11bootcamp.productservice.repositories;

import org.n11bootcamp.productservice.entities.OutboxEvent;
import org.n11bootcamp.productservice.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
