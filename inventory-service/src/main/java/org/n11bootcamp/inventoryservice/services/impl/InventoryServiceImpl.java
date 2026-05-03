package org.n11bootcamp.inventoryservice.services.impl;



import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.inventoryservice.dtos.requests.ReleaseStockRequest;
import org.n11bootcamp.inventoryservice.dtos.requests.ReserveStockRequest;
import org.n11bootcamp.inventoryservice.dtos.requests.UpdateStockRequest;
import org.n11bootcamp.inventoryservice.dtos.reponses.InventoryResponse;
import org.n11bootcamp.inventoryservice.entities.Inventory;
import org.n11bootcamp.inventoryservice.entities.OutboxEvent;
import org.n11bootcamp.inventoryservice.enums.AggregateType;
import org.n11bootcamp.inventoryservice.enums.EventType;

import org.n11bootcamp.inventoryservice.enums.OutboxStatus;
import org.n11bootcamp.inventoryservice.enums.TargetSystem;
import org.n11bootcamp.inventoryservice.exceptions.*;
import org.n11bootcamp.inventoryservice.repositories.*;

import org.n11bootcamp.inventoryservice.services.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for productId: " + productId));

        return toResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse createInventory(UUID productId, UUID sellerId,String sellerEmail, Integer initialQuantity) {

        if (inventoryRepository.existsByProductId(productId)) {
            throw new InventoryAlreadyExistsException(
                    "Inventory already exists for productId: " + productId);
        }

        Inventory inventory = Inventory.builder()
                .productId(productId)
                .sellerId(sellerId)
                .sellerEmail(sellerEmail)
                .availableQuantity(initialQuantity)
                .reservedQuantity(0)
                .build();

        Inventory saved = inventoryRepository.save(inventory);

//        saveOutboxEvent(
//                AggregateType.INVENTORY,
//                String.valueOf(saved.getId()),
//                EventType.INVENTORY_CREATED,
//                TargetSystem.NOTIFICATION_SERVICE,
//                saved
//        );

        log.info("Inventory created. productId={}, sellerId={}", productId, sellerId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public InventoryResponse updateStock(UUID productId, UUID sellerId, @Valid UpdateStockRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for productId: " + productId));

        if (!inventory.getSellerId().equals(sellerId)) {
            throw new SellerNotAuthorizedThisProductException("Seller is not authorized to update this product's stock.");
        }

        inventory.setAvailableQuantity(request.getQuantity());

        Inventory saved = inventoryRepository.save(inventory);

//        saveOutboxEvent(
//                AggregateType.INVENTORY,
//                String.valueOf(saved.getId()),
//                EventType.STOCK_UPDATED,
//                TargetSystem.NOTIFICATION_SERVICE,
//                saved
//        );

        log.info("Stock updated. productId={}, newQuantity={}", productId, request.getQuantity());
        return toResponse(saved);
    }


    @Override
    @Transactional
    public void reserveStock(@Valid ReserveStockRequest request) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for productId: " + request.getProductId()));

        if (inventory.getAvailableQuantity() < request.getQuantity()) {
            saveOutboxEvent(
                    AggregateType.INVENTORY,
                    inventory.getId().toString(),
                    EventType.STOCK_RESERVATION_FAILED,
                    TargetSystem.ORDER_SERVICE,
                    Map.of(
                            "orderId", request.getOrderId().toString(),
                            "productId", request.getProductId().toString(),
                            "reason", "Insufficient stock. Available: %d, Requested: %d"
                                    .formatted(inventory.getAvailableQuantity(), request.getQuantity()),
                            "sellerEmail", inventory.getSellerEmail()
                    )
            );
            log.warn("Insufficient stock. productId={}, available={}, requested={}",
                    request.getProductId(), inventory.getAvailableQuantity(), request.getQuantity());
            return;
        }

        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - request.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.getQuantity());
        inventoryRepository.save(inventory);

        saveOutboxEvent(
                AggregateType.INVENTORY,
                inventory.getId().toString(),
                EventType.STOCK_RESERVED,
                TargetSystem.ORDER_SERVICE,
                Map.of(
                        "orderId", request.getOrderId().toString(),
                        "productId", request.getProductId().toString(),
                        "reservedQuantity", request.getQuantity(),
                        "remainingAvailable", inventory.getAvailableQuantity()
                )
        );

        log.info("Stock reserved. productId={}, orderId={}, quantity={}",
                request.getProductId(), request.getOrderId(), request.getQuantity());
    }

    @Override
    @Transactional
    public void releaseStock(@Valid ReleaseStockRequest request) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for productId: " + request.getProductId()));

        if (inventory.getReservedQuantity() < request.getQuantity()) {
            throw new ExceededReservedAmountException(
                    "Release quantity exceeds reserved amount. Reserved: %d, Release: %d"
                            .formatted(inventory.getReservedQuantity(), request.getQuantity()));
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() - request.getQuantity());
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + request.getQuantity());
        Inventory savedInventory = inventoryRepository.save(inventory);

        saveOutboxEvent(
                AggregateType.INVENTORY,
                String.valueOf(inventory.getId()),
                EventType.STOCK_RELEASED,
                TargetSystem.ORDER_SERVICE,
                savedInventory
        );

        log.info("Stock released. productId={}, orderId={}, quantity={}",
                request.getProductId(), request.getOrderId(), request.getQuantity());
    }

    private void saveOutboxEvent(@Valid AggregateType aggregateType, String aggregateId, EventType eventType, TargetSystem targetSystem, Object payload) {

        String payloadJson = null;
        if (payload != null) {
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize outbox payload", e);
            }
        }

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .targetSystem(targetSystem)
                .payload(payloadJson)
                .status(OutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        outboxRepository.save(event);
        log.debug("Outbox event saved. aggregateId: {}, eventType: {}, target: {}",
                aggregateId, eventType, targetSystem);
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .sellerId(inventory.getSellerId())
                .sellerEmail(inventory.getSellerEmail())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .totalQuantity(inventory.getAvailableQuantity() + inventory.getReservedQuantity())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteInventory(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for productId: " + productId));

        inventoryRepository.delete(inventory);
        log.info("Inventory deleted. productId={}", productId);
    }

    @Override
    @Transactional
    public void confirmStock(UUID productId, UUID orderId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for productId: " + productId));

        // Idempotency — aynı order için daha önce işlendi mi?
        if (orderId.equals(inventory.getLastConfirmedOrderId())) {
            log.warn("Duplicate OrderConfirmedEvent ignored. orderId: {}, productId: {}",
                    orderId, productId);
            return;
        }

        if (inventory.getReservedQuantity() < quantity) {
            throw new IllegalStateException(
                    "Reserved quantity is less than confirmed quantity. Reserved: %d, Confirmed: %d"
                            .formatted(inventory.getReservedQuantity(), quantity));
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
        inventory.setLastConfirmedOrderId(orderId);
        inventoryRepository.save(inventory);

        log.info("Stock confirmed (sold). productId={}, quantity={}", productId, quantity);
    }
}
