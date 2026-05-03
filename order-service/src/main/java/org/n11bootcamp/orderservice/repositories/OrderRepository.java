package org.n11bootcamp.orderservice.repositories;

import org.n11bootcamp.orderservice.entities.Order;
import org.n11bootcamp.orderservice.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findAllByUserId(UUID userId, Pageable pageable);

    boolean existsByUserIdAndStatus(UUID userId, OrderStatus status);

    Optional<Order> findByIdAndStatus(UUID id, OrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithLock(@Param("id") UUID id);

    @Query("""
    SELECT DISTINCT o FROM Order o
    JOIN o.items i
    WHERE i.sellerId = :sellerId
    ORDER BY o.createdAt DESC
    """)
    Page<Order> findOrdersBySellerIdInItems(
            @Param("sellerId") UUID sellerId,
            Pageable pageable);
}