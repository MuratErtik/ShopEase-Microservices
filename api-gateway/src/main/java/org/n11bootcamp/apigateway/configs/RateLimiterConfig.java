package org.n11bootcamp.apigateway.configs;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;


import java.util.Optional;

@Configuration
public class RateLimiterConfig {


    //If it has token it will limit user token based otherwise ip based on
    @Bean
    @Primary
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders()
                    .getFirst(JwtUserPropagationFilter.USER_ID_HEADER);

            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }

            String ip = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                    .map(addr -> addr.getAddress().getHostAddress())
                    .orElse("unknown");

            return Mono.just("ip:" + ip);
        };
    }
}