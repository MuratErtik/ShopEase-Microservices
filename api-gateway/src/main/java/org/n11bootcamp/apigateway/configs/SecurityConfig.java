package org.n11bootcamp.apigateway.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import reactor.core.publisher.Flux;

import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource((org.springframework.web.cors.reactive.CorsConfigurationSource) corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges

                        .pathMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/users/register").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/users/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/users/refresh").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/users/logout").permitAll()


                        .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .pathMatchers("/fallback/**").permitAll()


                        .pathMatchers(HttpMethod.POST,   "/api/v1/products").hasRole("SELLER")
                        .pathMatchers(HttpMethod.PATCH,  "/api/v1/products/{id}").hasRole("SELLER")
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/products/{id}").hasRole("SELLER")


                        .pathMatchers(HttpMethod.GET, "/api/v1/products/my").hasRole("SELLER")
                        .pathMatchers(HttpMethod.GET, "/api/v1/products/my/{id}").hasRole("SELLER")


                        .pathMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()


                        .pathMatchers(HttpMethod.POST, "/api/v1/inventory/**").hasRole("SELLER")
                        .pathMatchers(HttpMethod.PUT,  "/api/v1/inventory/**").hasRole("SELLER")


                        .pathMatchers(HttpMethod.GET, "/api/v1/inventory/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/sellers/**").permitAll()


                        .pathMatchers(HttpMethod.GET, "/api/v1/orders/seller").hasRole("SELLER")
                        .pathMatchers(HttpMethod.POST, "/api/v1/orders").authenticated()
                        .pathMatchers(HttpMethod.GET,  "/api/v1/orders/**").authenticated()
                        .pathMatchers("/api/v1/cart/**").authenticated()


                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of("*"));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE","PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept","Origin","X-Requested-With","Cache-Control"));

        config.setAllowCredentials(true);

        config.setExposedHeaders(List.of("Authorization"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return Flux.empty();
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");

            return Flux.fromIterable(roles)
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role));
        });

        return converter;
    }
}
