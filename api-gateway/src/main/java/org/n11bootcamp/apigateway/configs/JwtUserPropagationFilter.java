package org.n11bootcamp.apigateway.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtUserPropagationFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER    = "X-User-ID";
    public static final String USER_EMAIL_HEADER = "X-User-Email";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .filter(principal -> principal instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(jwtAuth -> {
                    Jwt jwt = jwtAuth.getToken();

                    String userId = jwt.getSubject();
                    String email  = jwt.getClaimAsString("email");
                    String roles  = jwtAuth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(","));

                    //remove fake headers which has potentially comes from client
                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .headers(headers -> {
                                        headers.remove(USER_ID_HEADER);
                                        headers.remove(USER_EMAIL_HEADER);
                                        headers.remove(USER_ROLES_HEADER);
                                        headers.add(USER_ID_HEADER, userId);
                                        if (email != null) headers.add(USER_EMAIL_HEADER, email);
                                        if (!roles.isBlank()) headers.add(USER_ROLES_HEADER, roles);
                                    })
                                    .build())
                            .build();

                    return chain.filter(mutatedExchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
