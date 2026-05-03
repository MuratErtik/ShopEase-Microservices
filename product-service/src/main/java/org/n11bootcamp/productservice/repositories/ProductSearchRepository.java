package org.n11bootcamp.productservice.repositories;

import org.n11bootcamp.productservice.documents.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.math.BigDecimal;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {
    Page<ProductDocument> findByCategory(String category, Pageable pageable);

    Page<ProductDocument> findByBrandIgnoreCase(String brand, Pageable pageable);

    Page<ProductDocument> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}
