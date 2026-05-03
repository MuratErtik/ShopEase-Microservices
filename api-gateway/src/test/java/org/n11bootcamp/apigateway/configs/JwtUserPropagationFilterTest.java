package org.n11bootcamp.apigateway.configs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtUserPropagationFilterTest {

    private JwtUserPropagationFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtUserPropagationFilter();
        chain  = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }



    // Builds a minimal JWT. Claims with null value are simply omitted.
    private Jwt buildJwt(String subject, String email) {
        Jwt.Builder b = Jwt.withTokenValue("tok").header("alg", "none").subject(subject);
        if (email != null) b.claim("email", email);
        return b.build();
    }


    private ServerWebExchange exchangeWithPrincipal(MockServerHttpRequest request,
                                                     Mono<? extends Principal> principal) {
        MockServerWebExchange real  = MockServerWebExchange.from(request);
        ServerWebExchange     mock  = mock(ServerWebExchange.class);
        when(mock.getRequest()).thenReturn(request);
        doReturn(principal).when(mock).getPrincipal();
        when(mock.mutate()).thenAnswer(inv -> real.mutate());
        return mock;
    }



    @Test
    @DisplayName("Should inject X-User-ID, X-User-Email and X-User-Roles from the JWT")
    void shouldInjectUserHeadersFromJwt() {
        Jwt jwt = buildJwt("user-123", "user@example.com");
        JwtAuthenticationToken jwtAuth = new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_SELLER")));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders").build();
        ServerWebExchange exchange = exchangeWithPrincipal(request, Mono.just(jwtAuth));

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        var headers = captor.getValue().getRequest().getHeaders();
        assertThat(headers.getFirst(JwtUserPropagationFilter.USER_ID_HEADER)).isEqualTo("user-123");
        assertThat(headers.getFirst(JwtUserPropagationFilter.USER_EMAIL_HEADER)).isEqualTo("user@example.com");
        assertThat(headers.getFirst(JwtUserPropagationFilter.USER_ROLES_HEADER))
                .contains("ROLE_USER").contains("ROLE_SELLER");
    }

    @Test
    @DisplayName("Should strip fake user headers injected by the client before setting real ones")
    void shouldStripFakeUserHeadersFromClient() {
        Jwt jwt = buildJwt("real-user", "real@example.com");
        JwtAuthenticationToken jwtAuth = new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/cart")
                .header(JwtUserPropagationFilter.USER_ID_HEADER,    "FAKE-ID")
                .header(JwtUserPropagationFilter.USER_EMAIL_HEADER, "fake@evil.com")
                .header(JwtUserPropagationFilter.USER_ROLES_HEADER, "ROLE_ADMIN")
                .build();
        ServerWebExchange exchange = exchangeWithPrincipal(request, Mono.just(jwtAuth));

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        var headers = captor.getValue().getRequest().getHeaders();
        assertThat(headers.getFirst(JwtUserPropagationFilter.USER_ID_HEADER)).isEqualTo("real-user");
        assertThat(headers.getFirst(JwtUserPropagationFilter.USER_EMAIL_HEADER)).isEqualTo("real@example.com");
        // Only the legitimate role, not the faked ADMIN
        assertThat(headers.getFirst(JwtUserPropagationFilter.USER_ROLES_HEADER))
                .doesNotContain("ADMIN").contains("ROLE_USER");
    }



    @Test
    @DisplayName("Should not add X-User-Email header when the JWT email claim is absent")
    void shouldNotAddEmailHeaderWhenEmailClaimIsAbsent() {
        Jwt jwt = buildJwt("user-456", null);  // no email claim
        JwtAuthenticationToken jwtAuth = new JwtAuthenticationToken(jwt, List.of());

        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = exchangeWithPrincipal(request, Mono.just(jwtAuth));

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(captor.getValue().getRequest().getHeaders()
                .getFirst(JwtUserPropagationFilter.USER_EMAIL_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should not add X-User-Roles header when the JWT has no granted authorities")
    void shouldNotAddRolesHeaderWhenAuthoritiesAreEmpty() {
        Jwt jwt = buildJwt("user-789", "a@b.com");
        JwtAuthenticationToken jwtAuth = new JwtAuthenticationToken(jwt, List.of()); // no roles

        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = exchangeWithPrincipal(request, Mono.just(jwtAuth));

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        // Roles string is blank → header must not be added
        assertThat(captor.getValue().getRequest().getHeaders()
                .getFirst(JwtUserPropagationFilter.USER_ROLES_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should pass through unchanged when no principal is present (anonymous request)")
    void shouldPassThroughWhenNoPrincipal() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products").build();
        ServerWebExchange exchange = exchangeWithPrincipal(request, Mono.empty());

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain, times(1)).filter(any());
        assertThat(captor.getValue().getRequest().getHeaders()
                .getFirst(JwtUserPropagationFilter.USER_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should pass through when principal is not a JwtAuthenticationToken")
    void shouldPassThroughWhenPrincipalIsNotJwtAuthenticationToken() {
        Principal nonJwt = () -> "some-other-principal";

        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = exchangeWithPrincipal(request, Mono.just(nonJwt));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain, times(1)).filter(any());
    }



    @Test
    @DisplayName("Should have order HIGHEST_PRECEDENCE + 1")
    void shouldHaveOrderHighestPrecedencePlusOne() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
    }
}
