package org.n11bootcamp.apigateway.configs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        chain = mock(GatewayFilterChain.class);
        // Default: chain just completes
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Should generate a new UUID when correlation ID header is missing")
    void shouldGenerateCorrelationIdWhenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Capture the mutated exchange passed to the chain
        when(chain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange mutatedExchange = invocation.getArgument(0,
                    org.springframework.web.server.ServerWebExchange.class);

            String correlationId = mutatedExchange.getRequest()
                    .getHeaders()
                    .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);

            assertThat(correlationId)
                    .isNotNull()
                    .isNotBlank()
                    .matches("^[0-9a-f-]{36}$");

            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should preserve existing correlation ID when header is already present")
    void shouldPreserveExistingCorrelationId() {
        String existingId = "test-correlation-id-123";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange mutatedExchange = invocation.getArgument(0,
                    org.springframework.web.server.ServerWebExchange.class);

            String correlationId = mutatedExchange.getRequest()
                    .getHeaders()
                    .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);

            assertThat(correlationId).isEqualTo(existingId);
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should generate a new ID when correlation ID header is blank")
    void shouldGenerateNewIdWhenHeaderIsBlank() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "   ")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange mutatedExchange = invocation.getArgument(0,
                    org.springframework.web.server.ServerWebExchange.class);

            String correlationId = mutatedExchange.getRequest()
                    .getHeaders()
                    .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);

            assertThat(correlationId)
                    .isNotBlank()
                    .isNotEqualTo("   ");
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should add correlation ID to response headers after chain completes")
    void shouldAddCorrelationIdToResponse() {
        String existingId = "response-test-id";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Run filter then trigger response commit so beforeCommit callbacks fire
        StepVerifier.create(
                filter.filter(exchange, chain)
                        .then(exchange.getResponse().setComplete())  // ← commit tetikler
        ).verifyComplete();

        String responseCorrelationId = exchange.getResponse()
                .getHeaders()
                .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);

        assertThat(responseCorrelationId).isEqualTo(existingId);
    }

    @Test
    @DisplayName("Should have highest precedence order")
    void shouldHaveHighestPrecedenceOrder() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}