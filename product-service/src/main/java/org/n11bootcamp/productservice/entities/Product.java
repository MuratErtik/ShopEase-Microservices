package org.n11bootcamp.productservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.n11bootcamp.productservice.enums.Category;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_product_name_brand_color",
                        columnNames = {"name", "brand", "color"}
                )
        },
        indexes = {
                @Index(name = "idx_product_status",   columnList = "status"),
                @Index(name = "idx_product_category", columnList = "category"),
                @Index(name = "idx_product_name",     columnList = "name"),
                @Index(name = "idx_product_price",    columnList = "price")
        }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private UUID sellerId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 255)
    private String brand;

    @Column(nullable = false, length = 30)
    private String color;

    @Column(nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(length = 500)
    private String imageUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

