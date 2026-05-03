package org.n11bootcamp.apigateway.configs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserHeaderFilterTest {

    private UserHeaderFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new UserHeaderFilter();
        chain  = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }


    // Builds a JWT; null arguments for optional claims simply skip that claim.
    private Jwt buildJwt(String subject, String email, List<String> roles,
                          String givenName, String familyName) {
        Jwt.Builder b = Jwt.withTokenValue("tok").header("alg", "none").subject(subject);
        if (email      != null) b.claim("email",       email);
        if (roles      != null) b.claim("roles",       roles);
        if (givenName  != null) b.claim("given_name",  givenName);
        if (familyName != null) b.claim("family_name", familyName);
        return b.build();
    }

    // Runs the filter with a security context injected into the Reactor context.
    private Mono<Void> filterWithContext(ServerWebExchange exchange, SecurityContext ctx) {
        return filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder
                        .withSecurityContext(Mono.just(ctx)));
    }


    @Test
    @DisplayName("Should inject all five user headers when authenticated with a full JWT")
    void shouldInjectAllUserHeadersWhenAuthenticated() {
        Jwt jwt = buildJwt("user-123", "user@example.com",
                           List.of("USER", "SELLER"), "John", "Doe");
        SecurityContext ctx = new SecurityContextImpl(
                new JwtAuthenticationToken(jwt, List.of()));

        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/api/v1/orders").build());

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filterWithContext(exchange, ctx)).verifyComplete();

        var h = captor.getValue().getRequest().getHeaders();
        assertThat(h.getFirst("X-User-ID")).isEqualTo("user-123");
        assertThat(h.getFirst("X-User-Email")).isEqualTo("user@example.com");
        assertThat(h.getFirst("X-User-Roles")).contains("USER").contains("SELLER");
        assertThat(h.getFirst("X-User-Name")).isEqualTo("John");
        assertThat(h.getFirst("X-User-Surname")).isEqualTo("Doe");
    }


    @Test
    @DisplayName("Should inject an empty string for X-User-Email when the email claim is absent")
    void shouldInjectEmptyEmailWhenClaimAbsent() {
        Jwt jwt = buildJwt("user-456", null, List.of("USER"), "Jane", "Smith");
        SecurityContext ctx = new SecurityContextImpl(new JwtAuthenticationToken(jwt, List.of()));

        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/test").build());

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filterWithContext(exchange, ctx)).verifyComplete();

        assertThat(captor.getValue().getRequest().getHeaders().getFirst("X-User-Email"))
                .isEqualTo("");
    }

    @Test
    @DisplayName("Should inject an empty string for X-User-Roles when the roles claim is absent")
    void shouldInjectEmptyRolesWhenClaimAbsent() {
        Jwt jwt = buildJwt("user-789", "a@b.com", null, null, null);
        SecurityContext ctx = new SecurityContextImpl(new JwtAuthenticationToken(jwt, List.of()));

        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/test").build());

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filterWithContext(exchange, ctx)).verifyComplete();

        assertThat(captor.getValue().getRequest().getHeaders().getFirst("X-User-Roles"))
                .isEqualTo("");
    }

    @Test
    @DisplayName("Should inject empty strings for X-User-Name and X-User-Surname when claims are absent")
    void shouldInjectEmptyNameAndSurnameWhenClaimsAbsent() {
        Jwt jwt = buildJwt("user-000", "a@b.com", List.of(), null, null);
        SecurityContext ctx = new SecurityContextImpl(new JwtAuthenticationToken(jwt, List.of()));

        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/test").build());

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filterWithContext(exchange, ctx)).verifyComplete();

        var h = captor.getValue().getRequest().getHeaders();
        assertThat(h.getFirst("X-User-Name")).isEqualTo("");
        assertThat(h.getFirst("X-User-Surname")).isEqualTo("");
    }



    @Test
    @DisplayName("Should pass through unchanged when the Reactor security context is absent")
    void shouldPassThroughWhenNoSecurityContext() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/api/v1/products").build());

        // No contextWrite → ReactiveSecurityContextHolder returns empty
        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain, times(1)).filter(any());
    }

    @Test
    @DisplayName("Should pass through unchanged when authentication is null in the security context")
    void shouldPassThroughWhenAuthenticationIsNull() {
        SecurityContext ctx = new SecurityContextImpl(null);

        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/test").build());

        StepVerifier.create(filterWithContext(exchange, ctx)).verifyComplete();

        verify(chain, times(1)).filter(any());
    }

    @Test
    @DisplayName("Should pass through unchanged when the principal is not a Jwt instance")
    void shouldPassThroughWhenPrincipalIsNotJwt() {
        // TestingAuthenticationToken has a String principal, not a Jwt
        SecurityContext ctx = new SecurityContextImpl(
                new TestingAuthenticationToken("username", "credentials"));

        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/test").build());

        StepVerifier.create(filterWithContext(exchange, ctx)).verifyComplete();

        verify(chain, times(1)).filter(exchange);
    }



    @Test
    @DisplayName("Should have order -1")
    void shouldHaveOrderMinusOne() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }
}