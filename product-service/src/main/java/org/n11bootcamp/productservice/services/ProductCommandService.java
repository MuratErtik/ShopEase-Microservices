package org.n11bootcamp.productservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.productservice.dtos.events.ProductCreatedEventPayload;
import org.n11bootcamp.productservice.dtos.events.ProductEsEventPayload;
import org.n11bootcamp.productservice.dtos.requests.CreateProductRequest;
import org.n11bootcamp.productservice.dtos.requests.UpdateProductRequest;
import org.n11bootcamp.productservice.dtos.responses.ProductResponse;
import org.n11bootcamp.productservice.entities.OutboxEvent;
import org.n11bootcamp.productservice.entities.Product;
import org.n11bootcamp.productservice.enums.AggregateType;
import org.n11bootcamp.productservice.enums.EventType;
import org.n11bootcamp.productservice.enums.OutboxStatus;
import org.n11bootcamp.productservice.enums.TargetSystem;
import org.n11bootcamp.productservice.exceptions.ProductAlreadyExistsException;
import org.n11bootcamp.productservice.exceptions.ProductNotFoundException;
import org.n11bootcamp.productservice.exceptions.ProductOwnershipException;
import org.n11bootcamp.productservice.mappers.ProductMapper;
import org.n11bootcamp.productservice.repositories.OutboxEventRepository;
import org.n11bootcamp.productservice.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductCommandService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public ProductResponse createProduct(CreateProductRequest request, UUID sellerId,String sellerEmail,String fullName) {

        //make sure the product which wants to insert by seller does not exist in DB before
        validateIsProductUnique(request.getName(), request.getBrand(), request.getColor());

        Product product = productMapper.toEntity(request);
        product.setSellerId(sellerId);
        product.setCreatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);

        ProductCreatedEventPayload eventPayload = ProductCreatedEventPayload.builder()
                .productId(savedProduct.getId())
                .sellerId(savedProduct.getSellerId())
                .sellerEmail(sellerEmail)
                .initialQuantity(request.getInitialQuantity())
                .build();

        ProductEsEventPayload esPayload = ProductEsEventPayload.builder()
                .id(savedProduct.getId())
                .name(savedProduct.getName())
                .description(savedProduct.getDescription())
                .price(savedProduct.getPrice())
                .brand(savedProduct.getBrand())
                .color(savedProduct.getColor())
                .category(savedProduct.getCategory())
                .imageUrl(savedProduct.getImageUrl())
                .createdAt(savedProduct.getCreatedAt())
                .sellerId(sellerId.toString())
                .sellerEmail(sellerEmail)
                .sellerFullName(fullName)
                .build();

        // using this pattern for data consistency for elasticsearch
        saveOutboxEvent(
                AggregateType.PRODUCT,
                savedProduct.getId().toString(),
                EventType.PRODUCT_CREATED,
                TargetSystem.ELASTICSEARCH,
                esPayload
        );

        // this one is for inventory service
        saveOutboxEvent(
                AggregateType.PRODUCT,
                savedProduct.getId().toString(),
                EventType.PRODUCT_CREATED,
                TargetSystem.INVENTORY_SERVICE,
                eventPayload
        );

        log.info("Product created in DB, outbox event saved. id: {}", savedProduct.getId());
        return productMapper.toResponse(savedProduct);
    }

    public ProductResponse updateProduct(UUID id, UpdateProductRequest request, UUID sellerId) {

        Product product = findProductById(id);

        validateSellerOwnership(product, sellerId);

        //product name could not be changed with already exist products name except itself
        checkForDuplicateOnUpdate(product, request);

        productMapper.updateProductFields(product, request);

        Product updatedProduct = productRepository.save(product);

        // using this pattern for data consistency
        saveOutboxEvent(
                AggregateType.PRODUCT,
                updatedProduct.getId().toString(),
                EventType.PRODUCT_UPDATED,
                TargetSystem.ELASTICSEARCH,
                updatedProduct
        );

        log.info("Product updated in DB, outbox event saved. id: {}", updatedProduct.getId());
        return productMapper.toResponse(updatedProduct);
    }

    public void deleteProduct(UUID id, UUID sellerId) {

        Product product = findProductById(id);

        validateSellerOwnership(product, sellerId);

        productRepository.deleteById(id);

        // using this pattern for data consistency
        saveOutboxEvent(
                AggregateType.PRODUCT,
                id.toString(),
                EventType.PRODUCT_DELETED,
                TargetSystem.ELASTICSEARCH,
                null
        );

        saveOutboxEvent(
                AggregateType.PRODUCT,
                id.toString(),
                EventType.PRODUCT_DELETED,
                TargetSystem.INVENTORY_SERVICE,
                null
        );

        log.info("Product deleted from DB, outbox event saved. id: {}", id);
    }



    private void validateSellerOwnership(Product product, UUID sellerId) {
        if (!product.getSellerId().equals(sellerId)) {
            throw new ProductOwnershipException(
                    "Seller " + sellerId + " is not the owner of product " + product.getId()
            );
        }
    }

    private void saveOutboxEvent(
            AggregateType aggregateType,
            String aggregateId,
            EventType eventType,
            TargetSystem targetSystem,
            Object payload) {

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

    private void validateIsProductUnique(String name, String brand, String color) {
        if (productRepository.existsByNameAndBrandAndColorIgnoreCase(name, brand, color)) {
            throw new ProductAlreadyExistsException(
                    String.format("A product with name '%s', brand '%s', and color '%s' already exists.", name, brand, color)
            );
        }
    }

    private Product findProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    //while a product updating must check do not convert another product that already exists in DB
    private void checkForDuplicateOnUpdate(Product existing, UpdateProductRequest request) {
        boolean combinationChanged =
                !existing.getName().equalsIgnoreCase(request.getName()) ||
                        !existing.getBrand().equalsIgnoreCase(request.getBrand()) ||
                        !existing.getColor().equalsIgnoreCase(request.getColor());

        if (combinationChanged) {
            validateIsProductUnique(request.getName(), request.getBrand(), request.getColor());
        }
    }
}