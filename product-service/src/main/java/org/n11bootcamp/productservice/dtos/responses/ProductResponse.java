package org.n11bootcamp.productservice.dtos.responses;

import lombok.*;
import org.n11bootcamp.productservice.enums.Category;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String brand;
    private String color;
    private Category category;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID sellerId;
    private SellerInfo  seller;

}