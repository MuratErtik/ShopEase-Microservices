package org.n11bootcamp.apigateway.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String correlationId = request.getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        String userId        = request.getHeaders().getFirst(JwtUserPropagationFilter.USER_ID_HEADER);

        log.info("REQUEST  method={} path={} correlationId={} userId={}",
                request.getMethod(),
                request.getPath(),
                correlationId,
                userId != null ? userId : "anonymous"
        );

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("RESPONSE method={} path={} status={} duration={}ms correlationId={}",
                            request.getMethod(),
                            request.getPath(),
                            exchange.getResponse().getStatusCode(),
                            duration,
                            correlationId
                    );
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
