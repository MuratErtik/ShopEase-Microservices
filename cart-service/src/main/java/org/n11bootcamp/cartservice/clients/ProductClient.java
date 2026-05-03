package org.n11bootcamp.cartservice.clients;




import org.n11bootcamp.cartservice.configs.FeignConfig;
import org.n11bootcamp.cartservice.dtos.reponses.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "product-service",
        url = "${feign.clients.product-service.url}",
        configuration = FeignConfig.class
)
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ProductResponse getProduct(@PathVariable UUID id);
}
