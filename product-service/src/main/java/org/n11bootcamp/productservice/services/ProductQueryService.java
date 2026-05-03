package org.n11bootcamp.productservice.services;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.productservice.documents.ProductDocument;
import org.n11bootcamp.productservice.dtos.responses.PageResponse;
import org.n11bootcamp.productservice.dtos.responses.ProductResponse;
import org.n11bootcamp.productservice.enums.Category;
import org.n11bootcamp.productservice.exceptions.InvalidPriceRangeException;
import org.n11bootcamp.productservice.exceptions.ProductNotFoundException;
import org.n11bootcamp.productservice.exceptions.ProductOwnershipException;
import org.n11bootcamp.productservice.mappers.ProductMapper;
import org.n11bootcamp.productservice.repositories.ProductSearchRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductQueryService {

    private final ProductSearchRepository productSearchRepository;
    private final ProductMapper productMapper;
    private final ElasticsearchOperations elasticsearchOperations;

    public ProductResponse getProductById(UUID id) {
        return productSearchRepository.findById(id.toString())
                .map(productMapper::toResponseFromDocument)
                .orElseGet(() -> {
                    log.error("SYNC_ISSUE: Product {} missing in ES!", id);
                    //alertingService.notify("ES sync problem for: " + id); // will be implemented when loggingservice developing
                    log.warn("Product not found in Elasticsearch, falling back to PostgreSQL. id: {}", id);
                    throw new ProductNotFoundException("Product not found in read model: " + id);
                });
    }

    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m))
                .withPageable(pageable)
                .build();
        return searchAndMap(query, pageable);
    }

    public PageResponse<ProductResponse> searchProductsByName(String name, Pageable pageable) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.match(m -> m.field("name").query(name)))
                .withPageable(pageable)
                .build();
        return searchAndMap(query, pageable);
    }

    public PageResponse<ProductResponse> getProductsByCategory(Category category, Pageable pageable) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.term(t -> t.field("category").value(category.name())))
                .withPageable(pageable)
                .build();
        return searchAndMap(query, pageable);
    }

    public PageResponse<ProductResponse> getProductsByBrand(String brand, Pageable pageable) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.term(t -> t.field("brand").value(brand.toLowerCase())))
                .withPageable(pageable)
                .build();
        return searchAndMap(query, pageable);
    }

    public PageResponse<ProductResponse> getProductsByPriceRange(
            BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        validatePriceRange(minPrice, maxPrice);
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.range(r -> r.untyped(u -> {
                    u.field("price");
                    if (minPrice != null) u.gte(JsonData.of(minPrice));
                    if (maxPrice != null) u.lte(JsonData.of(maxPrice));
                    return u;
                })))
                .withPageable(pageable)
                .build();
        return searchAndMap(query, pageable);
    }

    public PageResponse<ProductResponse> getProductsBySeller(UUID sellerId, Pageable pageable) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.term(t -> t.field("sellerId").value(sellerId.toString())))
                .withPageable(pageable)
                .build();
        return searchAndMap(query, pageable);
    }

    public PageResponse<ProductResponse> filterProducts(
            Category category,
            String brand,
            String color,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        // use the nativequery for a many filter combinations
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        if (category != null) {
            boolQuery.filter(TermQuery.of(t -> t
                    .field("category")
                    .value(category.name()))._toQuery());
        }

        if (brand != null && !brand.isBlank()) {
            boolQuery.filter(TermQuery.of(t -> t
                    .field("brand")
                    .value(brand.toLowerCase()))._toQuery());
        }

        if (color != null && !color.isBlank()) {
            boolQuery.filter(TermQuery.of(t -> t
                    .field("color")
                    .value(color.toLowerCase()))._toQuery());
        }

        if (minPrice != null || maxPrice != null) {
            boolQuery.filter(f -> f
                    .range(r -> r
                            .untyped(u -> {
                                u.field("price");
                                if (minPrice != null) u.gte(JsonData.of(minPrice));
                                if (maxPrice != null) u.lte(JsonData.of(maxPrice));
                                return u;
                            })
                    )
            );
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(boolQuery.build()._toQuery())
                .withPageable(pageable)
                .build();

        return searchAndMap(query, pageable);
    }

    private PageResponse<ProductResponse> searchAndMap(NativeQuery query, Pageable pageable) {
        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);
        SearchPage<ProductDocument> page = SearchHitSupport.searchPageFor(hits, pageable);
        return PageResponse.of(page.map(hit -> productMapper.toResponseFromDocument(hit.getContent())));
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidPriceRangeException("minPrice cannot be greater than maxPrice");
        }
    }

    public ProductResponse getSellerProductById(UUID id, UUID sellerId) {
        ProductDocument document = productSearchRepository.findById(id.toString())
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));

        if (!document.getSellerId().equals(sellerId.toString())) {
            throw new ProductOwnershipException(
                    "Seller " + sellerId + " is not the owner of product " + id
            );
        }

        return productMapper.toResponseFromDocument(document);
    }
}
