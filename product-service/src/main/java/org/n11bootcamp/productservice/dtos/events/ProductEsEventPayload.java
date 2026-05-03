package org.n11bootcamp.productservice.dtos.events;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.n11bootcamp.productservice.enums.Category;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEsEventPayload {


    private UUID          id;
    private String        name;
    private String        description;
    private BigDecimal    price;
    private String        brand;
    private String        color;
    private Category      category;
    private String        imageUrl;
    private LocalDateTime createdAt;

    private String        sellerId;
    private String        sellerEmail;
    private String        sellerFullName;
}
