package org.n11bootcamp.productservice.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.n11bootcamp.productservice.enums.Category;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Product name cannot be blank")
    @Size(max = 255, message = "Product name cannot exceed 255 characters")
    private String name;

    @Size(max = 512, message = "Description cannot exceed 512 characters")
    private String description;

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid")
    private BigDecimal price;

    @NotBlank(message = "Brand cannot be blank")
    @Size(max = 255, message = "Brand cannot exceed 255 characters")
    private String brand;

    @NotBlank(message = "Color cannot be blank")
    @Size(max = 30, message = "Color cannot exceed 30 characters")
    private String color;

    @NotNull(message = "Category cannot be null")
    private Category category;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;

    @NotNull(message = "Initial quantity cannot be null")
    @Min(value = 0, message = "Initial quantity cannot be negative")
    private Integer initialQuantity;
}
