package org.n11bootcamp.productservice.mappers;

import org.n11bootcamp.productservice.documents.ProductDocument;
import org.n11bootcamp.productservice.dtos.requests.CreateProductRequest;
import org.n11bootcamp.productservice.dtos.requests.UpdateProductRequest;
import org.n11bootcamp.productservice.dtos.responses.ProductResponse;
import org.n11bootcamp.productservice.entities.Product;
import org.n11bootcamp.productservice.enums.Category;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .brand(product.getBrand())
                .color(product.getColor())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public Product toEntity(CreateProductRequest request) {
        return Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .brand(request.getBrand())
                .color(request.getColor())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public ProductDocument toDocument(Product product) {
        return ProductDocument.builder()
                .id(product.getId().toString())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .brand(product.getBrand())
                .color(product.getColor())
                .category(product.getCategory().name())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .build();
    }

    public ProductResponse toResponseFromDocument(ProductDocument document) {
        return ProductResponse.builder()
                .id(UUID.fromString(document.getId()))
                .name(document.getName())
                .description(document.getDescription())
                .price(document.getPrice())
                .brand(document.getBrand())
                .color(document.getColor())
                .category(Category.valueOf(document.getCategory()))
                .imageUrl(document.getImageUrl())
                .seller(document.getSeller())
                .sellerId(UUID.fromString(document.getSellerId()))
                .createdAt(document.getCreatedAt())
                .build();
    }

    public void updateProductFields(Product product, UpdateProductRequest request) {
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getColor() != null) product.setColor(request.getColor());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());
        product.setUpdatedAt(LocalDateTime.now());
    }
}
