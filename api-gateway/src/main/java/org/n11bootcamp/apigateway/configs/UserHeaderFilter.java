package org.n11bootcamp.apigateway.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class UserHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .defaultIfEmpty(new SecurityContextImpl())
                .flatMap(ctx -> {
                    var auth = ctx.getAuthentication();
                    if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                        String userId = jwt.getSubject();
                        String email = jwt.getClaimAsString("email");
                        String roles = String.join(",", jwt.getClaimAsStringList("roles") != null
                                ? jwt.getClaimAsStringList("roles")
                                : java.util.List.of());

                        ServerHttpRequest mutatedRequest = exchange.getRequest()
                                .mutate()
                                .header("X-User-ID",      userId)
                                .header("X-User-Email",   email != null ? email : "")
                                .header("X-User-Roles",   roles)
                                .header("X-User-Name",    jwt.getClaimAsString("given_name") != null
                                        ? jwt.getClaimAsString("given_name") : "")
                                .header("X-User-Surname", jwt.getClaimAsString("family_name") != null
                                        ? jwt.getClaimAsString("family_name") : "")
                                .build();

                        log.debug("User headers injected. userId={}, email={}", userId, email);
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}