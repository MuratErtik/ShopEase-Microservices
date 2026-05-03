package org.n11bootcamp.apigateway.configs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@WebFluxTest(FallbackController.class)
class FallbackControllerTest {

    // Overrides SecurityConfig for this test slice only
    // permits everything so the controller is reachable without a JWT
    @TestConfiguration
    static class DisabledSecurityConfig {

        @Bean
        SecurityWebFilterChain testSecurityFilterChain(ServerHttpSecurity http) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(ex -> ex.anyExchange().permitAll())
                    .build();
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Should return 503 status")
    void shouldReturn503Status() {
        webTestClient.get()
                .uri("/fallback/service-unavailable")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Should return correct response body fields")
    void shouldReturnCorrectResponseBody() {
        webTestClient.get()
                .uri("/fallback/service-unavailable")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Service Temporarily Unavailable")
                .jsonPath("$.message").isNotEmpty()
                .jsonPath("$.timestamp").isNotEmpty()
                .jsonPath("$.path").isEqualTo("/fallback/service-unavailable");
    }

    @Test
    @DisplayName("Should return application/json content type")
    void shouldReturnJsonContentType() {
        webTestClient.get()
                .uri("/fallback/service-unavailable")
                .exchange()
                .expectHeader().contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("Should return 503 for POST requests as well")
    void shouldReturn503ForPostRequest() {
        webTestClient.post()
                .uri("/fallback/service-unavailable")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Should include correlation ID in body when header is present")
    void shouldIncludeCorrelationIdWhenPresent() {
        String correlationId = "test-id-abc123";

        webTestClient.get()
                .uri("/fallback/service-unavailable")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                .exchange()
                .expectBody()
                .jsonPath("$.correlationId").isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should return null correlationId value when header is absent")
    void shouldReturnNullCorrelationIdWhenHeaderAbsent() {
        // The controller always puts the key; value is null when no header is sent.
        // Jackson serialises Map null values as "correlationId": null, so the path
        // exists — we assert the value is null, not that the path is missing.
        webTestClient.get()
                .uri("/fallback/service-unavailable")
                .exchange()
                .expectBody()
                .jsonPath("$.correlationId").value(v -> assertThat(v).isNull());
    }
}