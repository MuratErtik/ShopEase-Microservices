package org.n11bootcamp.apigateway.configs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;

import org.springframework.http.HttpStatus;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;



    @Test
    @DisplayName("GET /api/v1/products/** should be accessible without authentication")
    void getProductsShouldBePublic() {
        webTestClient.get()
                .uri("/api/v1/products/1")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /api/v1/inventory/** should be accessible without authentication")
    void getInventoryShouldBePublic() {
        webTestClient.get()
                .uri("/api/v1/inventory/1")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("POST /api/v1/users/register should be accessible without authentication")
    void postRegisterShouldBePublic() {
        webTestClient.post()
                .uri("/api/v1/users/register")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /actuator/health should be accessible without authentication")
    void actuatorHealthShouldBePublic() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /fallback/service-unavailable should be accessible without authentication")
    void fallbackShouldBePublic() {
        webTestClient.get()
                .uri("/fallback/service-unavailable")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }



    @Test
    @DisplayName("POST /api/v1/products/** should require authentication")
    void postProductsShouldRequireAuth() {
        webTestClient.post()
                .uri("/api/v1/products")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /api/v1/orders/** should require authentication")
    void getOrdersShouldRequireAuth() {
        webTestClient.get()
                .uri("/api/v1/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /api/v1/cart/** should require authentication")
    void getCartShouldRequireAuth() {
        webTestClient.get()
                .uri("/api/v1/cart")
                .exchange()
                .expectStatus().isUnauthorized();
    }



    @Test
    @DisplayName("POST /api/v1/products/** with USER role should be forbidden")
    void postProductsWithUserRoleShouldBeForbidden() {
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .authorities(new org.springframework.security.core.authority
                                .SimpleGrantedAuthority("ROLE_USER")))
                .post()
                .uri("/api/v1/products")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /api/v1/products/** with SELLER role should pass security")
    void postProductsWithSellerRoleShouldPassSecurity() {
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .authorities(new org.springframework.security.core.authority
                                .SimpleGrantedAuthority("ROLE_SELLER")))
                .post()
                .uri("/api/v1/products")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("PUT /api/v1/inventory/** with USER role should be forbidden")
    void putInventoryWithUserRoleShouldBeForbidden() {
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .authorities(new org.springframework.security.core.authority
                                .SimpleGrantedAuthority("ROLE_USER")))
                .put()
                .uri("/api/v1/inventory/1")
                .exchange()
                .expectStatus().isForbidden();
    }
}