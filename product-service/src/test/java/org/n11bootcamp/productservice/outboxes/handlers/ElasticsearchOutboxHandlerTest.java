package org.n11bootcamp.productservice.outboxes.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.productservice.documents.ProductDocument;
import org.n11bootcamp.productservice.dtos.events.ProductEsEventPayload;
import org.n11bootcamp.productservice.entities.OutboxEvent;
import org.n11bootcamp.productservice.entities.Product;
import org.n11bootcamp.productservice.enums.AggregateType;
import org.n11bootcamp.productservice.enums.EventType;
import org.n11bootcamp.productservice.enums.TargetSystem;
import org.n11bootcamp.productservice.mappers.ProductMapper;
import org.n11bootcamp.productservice.repositories.ProductSearchRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ElasticsearchOutboxHandlerTest {

    @InjectMocks
    private ElasticsearchOutboxHandler elasticsearchOutboxHandler;

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void it_should_return_elasticsearch_as_target_system() {
        then(elasticsearchOutboxHandler.getTargetSystem()).isEqualTo(TargetSystem.ELASTICSEARCH);
    }

    @Test
    @DisplayName("Should create fresh ES document when PRODUCT_CREATED event received")
    void it_should_create_product_in_elasticsearch() throws Exception {
        // given
        String aggregateId = UUID.randomUUID().toString();
        String payloadJson = "{\"id\":\"" + aggregateId + "\", \"name\":\"Test\"}";

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.PRODUCT)
                .aggregateId(aggregateId)
                .eventType(EventType.PRODUCT_CREATED)
                .payload(payloadJson)
                .build();

        ProductEsEventPayload payloadDto = new ProductEsEventPayload();
        payloadDto.setId(UUID.fromString(aggregateId));

        when(objectMapper.readValue(anyString(), eq(ProductEsEventPayload.class))).thenReturn(payloadDto);

        // when
        elasticsearchOutboxHandler.handle(event);

        // then
        verify(productSearchRepository).save(any(ProductDocument.class));
    }

    @Test
    @DisplayName("Should update existing ES document when found")
    void it_should_update_existing_product_in_elasticsearch() throws Exception {
        // given
        String aggregateId = UUID.randomUUID().toString();
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.PRODUCT)
                .aggregateId(aggregateId)
                .eventType(EventType.PRODUCT_UPDATED)
                .payload("{}")
                .build();

        Product product = new Product();
        ProductDocument existingDoc = new ProductDocument();

        when(objectMapper.readValue(anyString(), eq(Product.class))).thenReturn(product);
        when(productSearchRepository.findById(aggregateId)).thenReturn(Optional.of(existingDoc));

        // when
        elasticsearchOutboxHandler.handle(event);

        // then
        verify(productSearchRepository).save(existingDoc);
    }

    @Test
    @DisplayName("Should create fresh document on update if document not found (Upsert logic)")
    void it_should_create_fresh_product_when_update_received_but_not_found_in_es() throws Exception {
        // given
        String aggregateId = UUID.randomUUID().toString();
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.PRODUCT)
                .aggregateId(aggregateId)
                .eventType(EventType.PRODUCT_UPDATED)
                .payload("{}")
                .build();

        Product product = new Product();
        ProductDocument newDoc = new ProductDocument();

        when(objectMapper.readValue(anyString(), eq(Product.class))).thenReturn(product);
        when(productSearchRepository.findById(aggregateId)).thenReturn(Optional.empty());
        when(productMapper.toDocument(product)).thenReturn(newDoc);

        // when
        elasticsearchOutboxHandler.handle(event);

        // then
        verify(productSearchRepository).save(newDoc);
    }

    @Test
    @DisplayName("Should delete document from ES")
    void it_should_delete_product_from_elasticsearch() {
        // given
        String aggregateId = UUID.randomUUID().toString();
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.PRODUCT)
                .aggregateId(aggregateId)
                .eventType(EventType.PRODUCT_DELETED)
                .build();

        // when
        elasticsearchOutboxHandler.handle(event);

        // then
        verify(productSearchRepository).deleteById(aggregateId);
    }

    @Test
    @DisplayName("Should do nothing when aggregate type is not PRODUCT")
    void it_should_do_nothing_when_aggregate_type_is_not_product() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.ORDER)
                .eventType(EventType.PRODUCT_CREATED)
                .build();

        // when
        elasticsearchOutboxHandler.handle(event);

        // then
        verifyNoInteractions(productSearchRepository, productMapper, objectMapper);
    }



}