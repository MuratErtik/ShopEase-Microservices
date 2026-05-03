package org.n11bootcamp.cartservice.clients;



import org.n11bootcamp.cartservice.dtos.reponses.InventoryResponse;
import org.n11bootcamp.cartservice.configs.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "inventory-service",
        url = "${feign.clients.inventory-service.url}",
        configuration = FeignConfig.class
)
public interface InventoryClient {

    @GetMapping("/api/v1/inventories/{productId}")
    InventoryResponse getInventory(@PathVariable UUID productId);
}