package org.n11bootcamp.apigateway.configs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoggingFilterTest {

    private LoggingFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new LoggingFilter();
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Should complete the filter chain successfully")
    void shouldCompleteFilterChain() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/products")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "log-test-id")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should propagate downstream errors without swallowing them")
    void shouldPropagateDownstreamErrors() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/orders")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any())).thenReturn(
                Mono.error(new RuntimeException("downstream failure"))
        );

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorMatches(e -> e.getMessage().equals("downstream failure"))
                .verify();
    }

    @Test
    @DisplayName("Should complete chain for anonymous requests (no user ID header)")
    void shouldCompleteChainForAnonymousRequest() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/products")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should have order HIGHEST_PRECEDENCE + 2")
    void shouldHaveCorrectOrder() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 2);
    }

    @Test
    @DisplayName("Should complete chain with user ID and correlation ID headers present")
    void shouldCompleteChainWithUserId() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/cart")
                .header(JwtUserPropagationFilter.USER_ID_HEADER, "user-abc-123")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-xyz")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }
}