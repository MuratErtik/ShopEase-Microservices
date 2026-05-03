package org.n11bootcamp.inventoryservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.n11bootcamp.inventoryservice.configs.RequestContext;
import org.n11bootcamp.inventoryservice.dtos.requests.UpdateStockRequest;
import org.n11bootcamp.inventoryservice.dtos.reponses.InventoryResponse;
import org.n11bootcamp.inventoryservice.services.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory and stock management endpoints")
public class InventoryController {

    private final InventoryService inventoryService;
    private final RequestContext requestContext;

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory by product ID")
    @ApiResponse(responseCode = "200", description = "Inventory details retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Inventory not found for the given product ID")
    public ResponseEntity<InventoryResponse> getByProductId(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }

    @PatchMapping("/{productId}/stock")
    @Operation(summary = "Update stock quantity for a product")
    @ApiResponse(responseCode = "200", description = "Stock updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid stock update request")
    @ApiResponse(responseCode = "403", description = "Seller is not authorized to update this inventory")
    @ApiResponse(responseCode = "404", description = "Inventory or product not found")
    public ResponseEntity<InventoryResponse> updateStock(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateStockRequest request,
            HttpServletRequest httpRequest) {
        UUID sellerId = requestContext.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(inventoryService.updateStock(productId, sellerId, request));
    }
}