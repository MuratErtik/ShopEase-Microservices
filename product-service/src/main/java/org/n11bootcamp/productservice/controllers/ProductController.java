package org.n11bootcamp.productservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.n11bootcamp.productservice.configs.RequestContext;
import org.n11bootcamp.productservice.dtos.requests.CreateProductRequest;
import org.n11bootcamp.productservice.dtos.requests.UpdateProductRequest;
import org.n11bootcamp.productservice.dtos.responses.PageResponse;
import org.n11bootcamp.productservice.dtos.responses.ProductResponse;
import org.n11bootcamp.productservice.enums.Category;
import org.n11bootcamp.productservice.services.ProductCommandService;
import org.n11bootcamp.productservice.services.ProductQueryService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "Product management endpoints")
public class ProductController {

    private final ProductCommandService productCommandService;
    private final ProductQueryService productQueryService;
    private final RequestContext requestContext;

    // ── COMMAND ──────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new product")
    @ApiResponse(responseCode = "201", description = "Product created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "409", description = "Product with same name already exists")
    public ResponseEntity<ProductResponse> createProduct(
            @RequestBody @Valid CreateProductRequest request,
            HttpServletRequest httpRequest) {
        UUID sellerId = requestContext.getCurrentUserId(httpRequest);
        String sellerEmail = requestContext.getCurrentEmail(httpRequest);
        String sellerFullName = requestContext.getCurrentFullName(httpRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productCommandService.createProduct(request, sellerId,sellerEmail,sellerFullName));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update product by ID")
    @ApiResponse(responseCode = "200", description = "Product updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "409", description = "Product with same name already exists")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateProductRequest request,
            HttpServletRequest httpRequest) {
        UUID sellerId = requestContext.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(productCommandService.updateProduct(id, request, sellerId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product by ID")
    @ApiResponse(responseCode = "204", description = "Product deleted successfully")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID sellerId = requestContext.getCurrentUserId(httpRequest);
        productCommandService.deleteProduct(id, sellerId);
        return ResponseEntity.noContent().build();
    }

    // ── QUERY ─────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productQueryService.getProductById(id));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my products (seller)")
    public ResponseEntity<PageResponse<ProductResponse>> getMyProducts(
            HttpServletRequest httpRequest,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        UUID sellerId = requestContext.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(productQueryService.getProductsBySeller(sellerId, pageable));
    }

    @GetMapping("/my/{id}")
    @Operation(summary = "Get my product by ID (seller)")
    @ApiResponse(responseCode = "403", description = "Product does not belong to this seller")
    public ResponseEntity<ProductResponse> getMyProductById(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID sellerId = requestContext.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(productQueryService.getSellerProductById(id, sellerId));
    }

    @GetMapping
    @Operation(summary = "Get all products with pagination")
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(productQueryService.getAllProducts(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by name")
    public ResponseEntity<PageResponse<ProductResponse>> searchByName(
            @RequestParam String name,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(productQueryService.searchProductsByName(name, pageable));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category")
    public ResponseEntity<PageResponse<ProductResponse>> getByCategory(
            @PathVariable Category category,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(productQueryService.getProductsByCategory(category, pageable));
    }

    @GetMapping("/brand")
    @Operation(summary = "Get products by brand")
    public ResponseEntity<PageResponse<ProductResponse>> getByBrand(
            @RequestParam String brand,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(productQueryService.getProductsByBrand(brand, pageable));
    }

    @GetMapping("/price-range")
    @Operation(summary = "Get products by price range")
    @ApiResponse(responseCode = "400", description = "minPrice must be less than maxPrice")
    public ResponseEntity<PageResponse<ProductResponse>> getByPriceRange(
            @RequestParam @DecimalMin("0.0") BigDecimal minPrice,
            @RequestParam @DecimalMin("0.0") BigDecimal maxPrice,
            @PageableDefault(size = 10, sort = "price") Pageable pageable) {
        return ResponseEntity.ok(productQueryService.getProductsByPriceRange(minPrice, maxPrice, pageable));
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter products with multiple criteria", description = "All parameters are optional. Combine freely.")
    public ResponseEntity<PageResponse<ProductResponse>> filterProducts(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) @DecimalMin("0.0") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin("0.0") BigDecimal maxPrice,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(
                productQueryService.filterProducts(category, brand, color, minPrice, maxPrice, pageable));
    }

    @GetMapping("/sellers/{sellerId}")
    @Operation(summary = "Get all products by seller (public storefront)")
    public ResponseEntity<PageResponse<ProductResponse>> getProductsBySeller(
            @PathVariable UUID sellerId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(productQueryService.getProductsBySeller(sellerId, pageable));
    }
}

