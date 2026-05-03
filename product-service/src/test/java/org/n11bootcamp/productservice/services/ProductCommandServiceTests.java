package org.n11bootcamp.productservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.productservice.dtos.requests.CreateProductRequest;
import org.n11bootcamp.productservice.dtos.requests.UpdateProductRequest;
import org.n11bootcamp.productservice.dtos.responses.ProductResponse;
import org.n11bootcamp.productservice.entities.OutboxEvent;
import org.n11bootcamp.productservice.entities.Product;
import org.n11bootcamp.productservice.exceptions.ProductAlreadyExistsException;
import org.n11bootcamp.productservice.exceptions.ProductNotFoundException;
import org.n11bootcamp.productservice.exceptions.ProductOwnershipException;
import org.n11bootcamp.productservice.mappers.ProductMapper;
import org.n11bootcamp.productservice.repositories.OutboxEventRepository;
import org.n11bootcamp.productservice.repositories.ProductRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCommandServiceTests {

    @Mock private ProductRepository productRepository;
    @Mock private ProductMapper productMapper;
    @Mock private OutboxEventRepository outboxRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private ProductCommandService productCommandService;

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final String SELLER_EMAIL = "murat@ertik.me";

    @Test
    @DisplayName("createProduct should save product and create outbox events when product is unique")
    void createProduct_success() throws JsonProcessingException {
        CreateProductRequest request = CreateProductRequest.builder()
                .brand("Apple").name("iPhone").color("Blue").build();

        Product product = Product.builder().id(PRODUCT_ID).sellerId(SELLER_ID).name("iPhone").build();
        ProductResponse response = new ProductResponse();

        when(productRepository.existsByNameAndBrandAndColorIgnoreCase(any(), any(), any())).thenReturn(false);
        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productCommandService.createProduct(request, SELLER_ID, SELLER_EMAIL, "Murat Ertik");

        assertThat(result).isNotNull();
        verify(outboxRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("createProduct should throw ProductAlreadyExistsException when product already exists")
    void createProduct_alreadyExists() {
        CreateProductRequest request = CreateProductRequest.builder().name("X").brand("Y").color("Z").build();
        when(productRepository.existsByNameAndBrandAndColorIgnoreCase(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> productCommandService.createProduct(request, SELLER_ID, SELLER_EMAIL, "Murat"))
                .isInstanceOf(ProductAlreadyExistsException.class);
    }

    @Test
    @DisplayName("updateProduct should update and save outbox event when seller is owner")
    void updateProduct_success() throws JsonProcessingException {
        UpdateProductRequest request = UpdateProductRequest.builder().name("New Name").brand("Apple").color("Blue").build();
        Product existingProduct = Product.builder().id(PRODUCT_ID).sellerId(SELLER_ID).name("Old Name").brand("Apple").color("Blue").build();

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(existingProduct)).thenReturn(existingProduct);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        productCommandService.updateProduct(PRODUCT_ID, request, SELLER_ID);

        verify(productMapper).updateProductFields(existingProduct, request);
        verify(productRepository).save(existingProduct);
    }

    @Test
    @DisplayName("updateProduct should throw ProductOwnershipException when seller is not the owner")
    void updateProduct_unauthorized() {
        Product existingProduct = Product.builder().id(PRODUCT_ID).sellerId(UUID.randomUUID()).build();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

        assertThatThrownBy(() -> productCommandService.updateProduct(PRODUCT_ID, new UpdateProductRequest(), SELLER_ID))
                .isInstanceOf(ProductOwnershipException.class);
    }

    @Test
    @DisplayName("updateProduct should validate uniqueness when name/brand/color is changed")
    void updateProduct_uniquenessValidation() {
        // given: Mevcut ürün ve farklı değerlere sahip yeni bir request
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("New Name").brand("New Brand").color("New Color").build();

        Product existingProduct = Product.builder()
                .id(PRODUCT_ID).sellerId(SELLER_ID)
                .name("Old Name").brand("Old Brand").color("Old Color").build();

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

        // Mock: Veritabanında bu yeni kombinasyonun zaten var olduğunu simüle ediyoruz
        when(productRepository.existsByNameAndBrandAndColorIgnoreCase("New Name", "New Brand", "New Color"))
                .thenReturn(true);

        // when then
        assertThatThrownBy(() -> productCommandService.updateProduct(PRODUCT_ID, request, SELLER_ID))
                .isInstanceOf(ProductAlreadyExistsException.class);
    }

    @Test
    @DisplayName("deleteProduct should delete from DB and save two outbox events")
    void deleteProduct_success() {
        Product product = Product.builder().id(PRODUCT_ID).sellerId(SELLER_ID).build();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        productCommandService.deleteProduct(PRODUCT_ID, SELLER_ID);

        verify(productRepository).deleteById(PRODUCT_ID);
        verify(outboxRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("findProductById should throw exception when not found")
    void findProductById_notFound() {
        when(productRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productCommandService.deleteProduct(PRODUCT_ID, SELLER_ID))
                .isInstanceOf(ProductNotFoundException.class);
    }
}