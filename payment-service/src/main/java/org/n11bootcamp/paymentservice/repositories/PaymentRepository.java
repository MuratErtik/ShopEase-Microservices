package org.n11bootcamp.paymentservice.repositories;

import org.n11bootcamp.paymentservice.entities.Payment;
import org.n11bootcamp.paymentservice.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Idempotency
    boolean existsByOrderId(UUID orderId);

    Optional<Payment> findByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId")
    Optional<Payment> findByOrderIdWithLock(@Param("orderId") UUID orderId);

    Optional<Payment> findByOrderIdAndStatus(UUID orderId, PaymentStatus status);
}
