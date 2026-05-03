package org.n11bootcamp.productservice.documents;

import org.n11bootcamp.productservice.dtos.responses.SellerInfo;
import org.springframework.data.annotation.Id;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(indexName = "products", writeTypeHint = WriteTypeHint.FALSE)
@Setting(settingPath = "elasticsearch/settings.json") //for lowercase
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Keyword, normalizer = "lowercase_normalizer")
    private String brand;

    @Field(type = FieldType.Keyword, normalizer = "lowercase_normalizer")
    private String color;

    @Field(type = FieldType.Keyword, normalizer = "lowercase_normalizer")
    private String category;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;

    @Field(type = FieldType.Keyword)
    private String sellerId;

    @Field(type = FieldType.Object)
    private SellerInfo seller;
}