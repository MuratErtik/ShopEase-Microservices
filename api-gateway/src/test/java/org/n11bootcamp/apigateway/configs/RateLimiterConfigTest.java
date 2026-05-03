package org.n11bootcamp.apigateway.configs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

class RateLimiterConfigTest {

    private KeyResolver keyResolver;

    @BeforeEach
    void setUp() {
        keyResolver = new RateLimiterConfig().userOrIpKeyResolver();
    }

    @Test
    @DisplayName("Should resolve key by user ID when JWT header is present")
    void shouldResolveKeyByUserId() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/orders")
                .header(JwtUserPropagationFilter.USER_ID_HEADER, "user-uuid-999")
                .remoteAddress(new InetSocketAddress("192.168.1.1", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("user:user-uuid-999")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fall back to IP-based key when user ID header is absent")
    void shouldFallBackToIpWhenUserIdMissing() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/products")
                .remoteAddress(new InetSocketAddress("10.0.0.42", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("ip:10.0.0.42")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fall back to IP-based key when user ID header is blank")
    void shouldFallBackToIpWhenUserIdIsBlank() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/products")
                .header(JwtUserPropagationFilter.USER_ID_HEADER, "   ")
                .remoteAddress(new InetSocketAddress("10.0.0.55", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("ip:10.0.0.55")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should resolve to ip:unknown when remote address is null")
    void shouldResolveToUnknownWhenRemoteAddressIsNull() {
        // No .remoteAddress() so getRemoteAddress() returns null
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/products")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // After our NPE fix this should complete with "ip:unknown" instead of throwing
        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("ip:unknown")
                .verifyComplete();
    }
}