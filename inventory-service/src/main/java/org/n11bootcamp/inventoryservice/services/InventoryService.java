package org.n11bootcamp.inventoryservice.services;

import jakarta.validation.Valid;
import org.n11bootcamp.inventoryservice.dtos.reponses.InventoryResponse;
import org.n11bootcamp.inventoryservice.dtos.requests.ReleaseStockRequest;
import org.n11bootcamp.inventoryservice.dtos.requests.ReserveStockRequest;
import org.n11bootcamp.inventoryservice.dtos.requests.UpdateStockRequest;

import java.util.UUID;

public interface InventoryService {


    InventoryResponse createInventory(UUID productId, UUID sellerId,String sellerEmail, Integer initialQuantity);

    InventoryResponse getByProductId(UUID productId);

    InventoryResponse updateStock(UUID productId, UUID sellerId, @Valid UpdateStockRequest request);

    void reserveStock(@Valid ReserveStockRequest request);

    void releaseStock(@Valid ReleaseStockRequest request);

    void deleteInventory(@Valid UUID productId);

    void confirmStock(@Valid UUID productId,UUID orderId, Integer quantity);
}
