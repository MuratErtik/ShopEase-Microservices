package org.n11bootcamp.productservice.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.productservice.documents.ProductDocument;
import org.n11bootcamp.productservice.dtos.responses.PageResponse;
import org.n11bootcamp.productservice.dtos.responses.ProductResponse;
import org.n11bootcamp.productservice.enums.Category;
import org.n11bootcamp.productservice.exceptions.InvalidPriceRangeException;
import org.n11bootcamp.productservice.exceptions.ProductNotFoundException;
import org.n11bootcamp.productservice.mappers.ProductMapper;
import org.n11bootcamp.productservice.repositories.ProductSearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.query.Query;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductQueryServiceTest {

    @InjectMocks
    private ProductQueryService productQueryService;

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;


    @SuppressWarnings("unchecked")
    private SearchHits<ProductDocument> mockSearchHits() {
        return mock(SearchHits.class);
    }



    @SuppressWarnings("unchecked")
    private void setupSearchPipeline(
            SearchHits<ProductDocument> searchHits,
            Pageable pageable,
            int count,
            MockedStatic<SearchHitSupport> mockedStatic) {

        SearchPage<ProductDocument> searchPage = mock(SearchPage.class);
        Page<ProductResponse> responsePage = new PageImpl<>(
                Collections.nCopies(count, new ProductResponse()), pageable, count);

        when(elasticsearchOperations.search((Query) any(), eq(ProductDocument.class)))
                .thenReturn(searchHits);
        mockedStatic.when(() -> SearchHitSupport.searchPageFor(searchHits, pageable))
                .thenReturn(searchPage);
        doReturn(responsePage).when(searchPage).map(any());
    }

    @Test
    public void it_should_return_product_when_found_in_elasticsearch() {

        // given
        UUID productId = UUID.randomUUID();

        ProductDocument document = new ProductDocument();
        ProductResponse expectedResponse = ProductResponse.builder()
                .id(productId)
                .name("Laptop")
                .build();

        when(productSearchRepository.findById(productId.toString())).thenReturn(Optional.of(document));
        when(productMapper.toResponseFromDocument(document)).thenReturn(expectedResponse);

        // when
        ProductResponse response = productQueryService.getProductById(productId);

        // then
        then(response).isNotNull();
        then(response.getId()).isEqualTo(productId);
        then(response.getName()).isEqualTo("Laptop");
    }

    @Test
    public void it_should_throw_exception_when_product_not_found_in_elasticsearch() {

        // given
        UUID nonExistentId = UUID.randomUUID();

        when(productSearchRepository.findById(nonExistentId.toString())).thenReturn(Optional.empty());

        // when
        Throwable throwable = catchThrowable(() -> productQueryService.getProductById(nonExistentId));

        // then
        then(throwable).isInstanceOf(ProductNotFoundException.class);
        verifyNoInteractions(productMapper);
    }

    public static Stream<Arguments> pageable_requests() {
        return Stream.of(
                Arguments.of(PageRequest.of(0, 10), 3),
                Arguments.of(PageRequest.of(0, 5),  1),
                Arguments.of(PageRequest.of(1, 10), 0)
        );
    }

    @ParameterizedTest
    @MethodSource("pageable_requests")
    public void it_should_return_all_products_with_pagination(Pageable pageable, int productCount) {

        // given
        SearchHits<ProductDocument> searchHits = mockSearchHits();

        try (var mockedStatic = mockStatic(SearchHitSupport.class)) {
            setupSearchPipeline(searchHits, pageable, productCount, mockedStatic);

            // when
            PageResponse<ProductResponse> response = productQueryService.getAllProducts(pageable);

            // then
            then(response).isNotNull();
            then(response.getContent()).hasSize(productCount);
        }
    }


    public static Stream<Arguments> category_requests() {
        return Stream.of(
                Arguments.of(Category.ELECTRONICS, 5),
                Arguments.of(Category.FASHION,     2),
                Arguments.of(Category.HOMELIVING,  0)
        );
    }

    @ParameterizedTest
    @MethodSource("category_requests")
    public void it_should_return_products_by_category(Category category, int expectedCount) {

        // given
        Pageable pageable = PageRequest.of(0, 10);
        SearchHits<ProductDocument> searchHits = mockSearchHits();

        try (var mockedStatic = mockStatic(SearchHitSupport.class)) {
            setupSearchPipeline(searchHits, pageable, expectedCount, mockedStatic);

            // when
            PageResponse<ProductResponse> response =
                    productQueryService.getProductsByCategory(category, pageable);

            // then
            then(response).isNotNull();
            then(response.getContent()).hasSize(expectedCount);
        }
    }

    public static Stream<Arguments> brand_requests() {
        return Stream.of(
                Arguments.of("Apple",   3),
                Arguments.of("Samsung", 2),
                Arguments.of("Unknown", 0)
        );
    }

    @ParameterizedTest
    @MethodSource("brand_requests")
    public void it_should_return_products_by_brand(String brand, int expectedCount) {

        // given
        Pageable pageable = PageRequest.of(0, 10);
        SearchHits<ProductDocument> searchHits = mockSearchHits();

        try (var mockedStatic = mockStatic(SearchHitSupport.class)) {
            setupSearchPipeline(searchHits, pageable, expectedCount, mockedStatic);

            // when
            PageResponse<ProductResponse> response =
                    productQueryService.getProductsByBrand(brand, pageable);

            // then
            then(response).isNotNull();
            then(response.getContent()).hasSize(expectedCount);
        }
    }


    public static Stream<Arguments> valid_price_range_requests() {
        return Stream.of(
                Arguments.of(BigDecimal.valueOf(10),   BigDecimal.valueOf(100),  3),
                Arguments.of(BigDecimal.valueOf(100),  BigDecimal.valueOf(500),  2),
                Arguments.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(5000), 0)
        );
    }

    @ParameterizedTest
    @MethodSource("valid_price_range_requests")
    public void it_should_return_products_by_price_range(
            BigDecimal minPrice, BigDecimal maxPrice, int expectedCount) {

        // given
        Pageable pageable = PageRequest.of(0, 10);
        SearchHits<ProductDocument> searchHits = mockSearchHits();

        try (var mockedStatic = mockStatic(SearchHitSupport.class)) {
            setupSearchPipeline(searchHits, pageable, expectedCount, mockedStatic);

            // when
            PageResponse<ProductResponse> response =
                    productQueryService.getProductsByPriceRange(minPrice, maxPrice, pageable);

            // then
            then(response).isNotNull();
            then(response.getContent()).hasSize(expectedCount);
        }
    }


    public static Stream<Arguments> invalid_price_range_requests() {
        return Stream.of(
                Arguments.of(BigDecimal.valueOf(500), BigDecimal.valueOf(100)),
                Arguments.of(BigDecimal.valueOf(999), BigDecimal.valueOf(1)),
                Arguments.of(BigDecimal.valueOf(50),  BigDecimal.valueOf(49.99))
        );
    }

    @ParameterizedTest
    @MethodSource("invalid_price_range_requests")
    public void it_should_fail_when_min_price_is_greater_than_max_price(
            BigDecimal minPrice, BigDecimal maxPrice) {

        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Throwable throwable = catchThrowable(() ->
                productQueryService.getProductsByPriceRange(minPrice, maxPrice, pageable));

        // then
        then(throwable).isInstanceOf(InvalidPriceRangeException.class);

        verifyNoInteractions(elasticsearchOperations);
        verifyNoInteractions(productMapper);
    }


    @Test
    public void it_should_allow_null_min_price_in_price_range() {

        // given
        Pageable pageable = PageRequest.of(0, 10);
        BigDecimal maxPrice = BigDecimal.valueOf(500);
        SearchHits<ProductDocument> searchHits = mockSearchHits();

        try (var mockedStatic = mockStatic(SearchHitSupport.class)) {
            setupSearchPipeline(searchHits, pageable, 0, mockedStatic);

            // when
            Throwable throwable = catchThrowable(() ->
                    productQueryService.getProductsByPriceRange(null, maxPrice, pageable));

            // then — exception fırlatılmamalı
            then(throwable).isNull();
        }
    }

    @Test
    public void it_should_allow_null_max_price_in_price_range() {

        // given
        Pageable pageable = PageRequest.of(0, 10);
        BigDecimal minPrice = BigDecimal.valueOf(100);
        SearchHits<ProductDocument> searchHits = mockSearchHits();

        try (var mockedStatic = mockStatic(SearchHitSupport.class)) {
            setupSearchPipeline(searchHits, pageable, 0, mockedStatic);

            // when
            Throwable throwable = catchThrowable(() ->
                    productQueryService.getProductsByPriceRange(minPrice, null, pageable));

            // then — exception fırlatılmamalı
            then(throwable).isNull();
        }
    }


    @Test
    public void it_should_filter_products_using_native_query() {

        // given
        Pageable pageable = PageRequest.of(0, 10);
        SearchHits<ProductDocument> searchHits = mockSearchHits();

        try (var mockedStatic = mockStatic(SearchHitSupport.class)) {
            setupSearchPipeline(searchHits, pageable, 2, mockedStatic);

            // when
            productQueryService.filterProducts(
                    Category.ELECTRONICS, "Apple", "Silver",
                    BigDecimal.valueOf(100), BigDecimal.valueOf(2000),
                    pageable);
        }

        // then
        verify(elasticsearchOperations).search((Query) any(), eq(ProductDocument.class));
    }

    @Test
    public void it_should_filter_products_with_all_null_filters() {

        // given
        Pageable pageable = PageRequest.of(0, 10);
        SearchHits<ProductDocument> searchHits = mockSearchHits();

        try (var mockedStatic = mockStatic(SearchHitSupport.class)) {
            setupSearchPipeline(searchHits, pageable, 0, mockedStatic);

            // when
            productQueryService.filterProducts(null, null, null, null, null, pageable);
        }

        // then
        verify(elasticsearchOperations).search((Query) any(), eq(ProductDocument.class));
        verifyNoInteractions(productSearchRepository);
    }
}