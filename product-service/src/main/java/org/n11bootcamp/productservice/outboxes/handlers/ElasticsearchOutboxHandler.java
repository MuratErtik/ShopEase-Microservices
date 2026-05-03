package org.n11bootcamp.productservice.outboxes.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.productservice.documents.ProductDocument;

import org.n11bootcamp.productservice.dtos.events.ProductEsEventPayload;
import org.n11bootcamp.productservice.dtos.responses.SellerInfo;
import org.n11bootcamp.productservice.entities.OutboxEvent;
import org.n11bootcamp.productservice.entities.Product;
import org.n11bootcamp.productservice.enums.AggregateType;
import org.n11bootcamp.productservice.enums.TargetSystem;
import org.n11bootcamp.productservice.mappers.ProductMapper;
import org.n11bootcamp.productservice.outboxes.OutboxEventHandler;
import org.n11bootcamp.productservice.repositories.ProductSearchRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchOutboxHandler implements OutboxEventHandler {

    private final ProductSearchRepository productSearchRepository;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;

    @Override
    public TargetSystem getTargetSystem() {
        return TargetSystem.ELASTICSEARCH;
    }

    @Override
    public void handle(OutboxEvent event) {
        if (event.getAggregateType() == AggregateType.PRODUCT) {
            handleProductEvent(event);
        } else {
            log.warn("Unhandled aggregateType: {} in ElasticsearchOutboxHandler",
                    event.getAggregateType());
        }
    }

    private void handleProductEvent(OutboxEvent event) {
        switch (event.getEventType()) {
            case PRODUCT_CREATED -> createProduct(event);
            case PRODUCT_UPDATED -> updateProduct(event);
            case PRODUCT_DELETED -> deleteProduct(event);
            default -> log.warn("Unhandled eventType: {}", event.getEventType());
        }
    }


    private void createProduct(OutboxEvent event) {
        try {
            ProductEsEventPayload payload = deserialize(event.getPayload(),
                    ProductEsEventPayload.class);

            SellerInfo sellerInfo = SellerInfo.builder()
                    .id(payload.getSellerId())
                    .email(payload.getSellerEmail())
                    .fullName(payload.getSellerFullName())
                    .build();

            ProductDocument document = ProductDocument.builder()
                    .id(payload.getId().toString())
                    .name(payload.getName())
                    .description(payload.getDescription())
                    .price(payload.getPrice())
                    .brand(payload.getBrand())
                    .color(payload.getColor())
                    .category(payload.getCategory() != null
                            ? payload.getCategory().name() : null)
                    .imageUrl(payload.getImageUrl())
                    .createdAt(payload.getCreatedAt())
                    .sellerId(payload.getSellerId())   // flat keyword
                    .seller(sellerInfo)                // nested
                    .build();

            productSearchRepository.save(document);
            log.info("ES document created. id={}", event.getAggregateId());

        } catch (Exception e) {
            log.error("Failed to create ES document. id={}", event.getAggregateId(), e);
            throw new RuntimeException(e);
        }
    }


    private void updateProduct(OutboxEvent event) {
        try {
            Product product = deserialize(event.getPayload(), Product.class);

            ProductDocument existing = productSearchRepository
                    .findById(event.getAggregateId())
                    .orElse(null);

            if (existing == null) {
                log.warn("ES document not found for update, creating fresh. id={}",
                        event.getAggregateId());
                productSearchRepository.save(productMapper.toDocument(product));
                return;
            }

            existing.setName(product.getName());
            existing.setDescription(product.getDescription());
            existing.setPrice(product.getPrice());
            existing.setBrand(product.getBrand());
            existing.setColor(product.getColor());
            existing.setCategory(product.getCategory() != null
                    ? product.getCategory().name() : null);
            existing.setImageUrl(product.getImageUrl());

            productSearchRepository.save(existing);
            log.info("ES document updated. id={}", event.getAggregateId());

        } catch (Exception e) {
            log.error("Failed to update ES document. id={}", event.getAggregateId(), e);
            throw new RuntimeException(e);
        }
    }


    private void deleteProduct(OutboxEvent event) {
        productSearchRepository.deleteById(event.getAggregateId());
        log.info("ES document deleted. id={}", event.getAggregateId());
    }


    private <T> T deserialize(Object raw, Class<T> type) throws Exception {
        if (raw instanceof String str) {
            return objectMapper.readValue(str, type);
        }
        return objectMapper.convertValue(raw, type);
    }
}