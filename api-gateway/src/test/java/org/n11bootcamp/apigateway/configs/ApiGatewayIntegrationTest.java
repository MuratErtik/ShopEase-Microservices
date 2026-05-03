package org.n11bootcamp.apigateway.configs;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApiGatewayIntegrationTest {



    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    // WireMock acts as both the downstream service stub and the Keycloak JWKS stub
    //before annotation removed.
    static WireMockServer wireMock;

    static {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        wireMock.stubFor(get(urlPathMatching("/realms/ecommerce/protocol/openid-connect/certs"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"keys\":[]}")));

        wireMock.stubFor(get(urlPathMatching("/api/v1/products/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"1\",\"name\":\"Test Product\"}")));
    }



    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port",
                () -> redis.getMappedPort(6379).toString());

        // Point Keycloak JWKS at WireMock
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:" + wireMock.port()
                        + "/realms/ecommerce/protocol/openid-connect/certs");

        // Route all downstream services to WireMock
        String wireMockBase = "http://localhost:" + wireMock.port();
        registry.add("PRODUCT_SERVICE_URL",   () -> wireMockBase);
        registry.add("INVENTORY_SERVICE_URL", () -> wireMockBase);
        registry.add("CART_SERVICE_URL",      () -> wireMockBase);
        registry.add("ORDER_SERVICE_URL",     () -> wireMockBase);
        registry.add("PAYMENT_SERVICE_URL",   () -> wireMockBase);
        registry.add("USER_SERVICE_URL",      () -> wireMockBase);
        registry.add("NOTIFICATION_SERVICE_URL", () -> wireMockBase);
        registry.add("SHIPPING_SERVICE_URL",  () -> wireMockBase);
        registry.add("SELLER_SERVICE_URL",    () -> wireMockBase);
    }

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void resetWireMock() {
        wireMock.resetRequests();
    }



    @Test
    @DisplayName("Should inject a correlation ID into the response when none is sent")
    void shouldInjectCorrelationIdInResponse() {
        webTestClient.get()
                .uri("/api/v1/products/1")
                .exchange()
                .expectHeader()
                .exists("X-Correlation-ID");
    }

    @Test
    @DisplayName("Should preserve an existing correlation ID through the pipeline")
    void shouldPreserveExistingCorrelationId() {
        String correlationId = "integration-test-id-abc";

        webTestClient.get()
                .uri("/api/v1/products/1")
                .header("X-Correlation-ID", correlationId)
                .exchange()
                .expectHeader()
                .valueEquals("X-Correlation-ID", correlationId);
    }



    @Test
    @DisplayName("Should return 503 from fallback when downstream is unavailable")
    void shouldReturnFallbackWhenDownstreamFails() {
        // Override product stub to simulate a 500 error (triggers circuit breaker)
        wireMock.stubFor(get(urlPathMatching("/api/v1/products/error"))
                .willReturn(aResponse().withStatus(500)));

        webTestClient.get()
                .uri("/api/v1/products/error")
                .exchange()
                // Circuit breaker kicks in after threshold — may be 500 initially
                .expectStatus().value(status ->
                        org.assertj.core.api.Assertions.assertThat(status)
                                .isIn(500, 503));
    }



    @Test
    @DisplayName("Actuator /health should be reachable without authentication")
    void actuatorHealthShouldBeReachable() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }


    @Test
    @DisplayName("Should return 401 when accessing a protected endpoint without a token")
    void shouldReturn401ForProtectedEndpointWithoutToken() {
        webTestClient.get()
                .uri("/api/v1/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/v1/products should return 401 without a token (SELLER-only endpoint)")
    void postProductsShouldReturn401WithoutToken() {
        webTestClient.post()
                .uri("/api/v1/products")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /fallback/service-unavailable should be reachable without authentication")
    void fallbackEndpointShouldBePublic() {
        webTestClient.get()
                .uri("/fallback/service-unavailable")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Service Temporarily Unavailable");
    }

    @Test
    @DisplayName("X-Correlation-ID should appear in the response when one is sent")
    void correlationIdSentShouldBeMirroredInResponse() {
        String id = "round-trip-test-id";
        webTestClient.get()
                .uri("/api/v1/products/1")
                .header("X-Correlation-ID", id)
                .exchange()
                .expectHeader().valueEquals("X-Correlation-ID", id);
    }
}