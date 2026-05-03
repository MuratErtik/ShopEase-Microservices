package org.n11bootcamp.productservice.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.n11bootcamp.productservice.enums.Category;

import java.math.BigDecimal;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @Size(max = 255, message = "Product name cannot exceed 255 characters")
    private String name;

    @Size(max = 512, message = "Description cannot exceed 512 characters")
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid")
    private BigDecimal price;

    @Size(max = 255, message = "Brand cannot exceed 255 characters")
    private String brand;

    @Size(max = 30, message = "Color cannot exceed 30 characters")
    private String color;

    private Category category;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;
}
