package org.n11bootcamp.userservice.services;





import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.n11bootcamp.userservice.configs.KeycloakConfig;
import org.n11bootcamp.userservice.dtos.repsonses.TokenResponse;
import org.n11bootcamp.userservice.dtos.requests.*;
import org.n11bootcamp.userservice.exceptions.InvalidCredentialsException;
import org.n11bootcamp.userservice.exceptions.UserAlreadyExistsException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.DataInput;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    private final KeycloakConfig keycloakConfig;
    private final Keycloak keycloakAdminClient;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;



    public UUID createKeycloakUser(RegisterRequest request) {
        UserRepresentation keycloakUser = buildKeycloakUser(request);
        RealmResource realmResource = keycloakAdminClient.realm(keycloakConfig.getRealm());

        try (Response response = realmResource.users().create(keycloakUser)) {

            if (response.getStatus() == 409) {
                throw new UserAlreadyExistsException(
                        "User already exists in Keycloak: " + request.getEmail()
                );
            }

            if (response.getStatus() != 201) {
                throw new RuntimeException(
                        "Failed to create Keycloak user. Status: " + response.getStatus()
                );
            }

            String locationHeader = response.getHeaderString("Location");
            String keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);

            assignRole(realmResource, keycloakUserId, request.getRole().name());

            log.info("Keycloak user created successfully. keycloakId: {}", keycloakUserId);
            return UUID.fromString(keycloakUserId);
        }
    }



    public TokenResponse login(LoginRequest request) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("username", request.getEmail());
        formData.add("password", request.getPassword());

        String rawResponse = webClientBuilder.build()
                .post()
                .uri(buildTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        clientResponse -> Mono.error(
                                new InvalidCredentialsException("Invalid email or password")
                        )
                )
                .bodyToMono(String.class)
                .block();

        log.debug("Raw response: {}", rawResponse);

        try {
            JsonNode node = objectMapper.readTree(rawResponse);
            log.debug("access_token: {}", node.get("access_token"));
            log.debug("refresh_token: {}", node.get("refresh_token"));

            return TokenResponse.builder()
                    .accessToken(node.get("access_token") != null ? node.get("access_token").asText() : null)
                    .refreshToken(node.get("refresh_token") != null ? node.get("refresh_token").asText() : null)
                    .expiresIn(node.get("expires_in") != null ? node.get("expires_in").asLong() : null)
                    .tokenType(node.get("token_type") != null ? node.get("token_type").asText() : null)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token response", e);
        }
    }



    public TokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("refresh_token", refreshToken);

        return webClientBuilder.build()
                .post()
                .uri(buildTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        clientResponse -> Mono.error(
                                new InvalidCredentialsException("Invalid or expired refresh token")
                        )
                )
                .bodyToMono(KeycloakTokenResponse.class)
                .map(this::toTokenResponse)
                .block();
    }



    public void logout(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("refresh_token", refreshToken);

        webClientBuilder.build()
                .post()
                .uri(buildLogoutUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(Void.class)
                .block();

        log.info("User logged out successfully");
    }



    private UserRepresentation buildKeycloakUser(RegisterRequest request) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getEmail());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setCredentials(List.of(credential));

        return user;
    }

    private void assignRole(RealmResource realmResource, String keycloakUserId, String roleName) {
        RoleRepresentation role = realmResource.roles()
                .get(roleName)
                .toRepresentation();

        realmResource.users()
                .get(keycloakUserId)
                .roles()
                .realmLevel()
                .add(List.of(role));

        log.info("Role '{}' assigned to keycloakId: {}", roleName, keycloakUserId);
    }

    private String buildTokenUrl() {
        return keycloakConfig.getServerUrl()
                + "/realms/" + keycloakConfig.getRealm()
                + "/protocol/openid-connect/token";
    }

    private String buildLogoutUrl() {
        return keycloakConfig.getServerUrl()
                + "/realms/" + keycloakConfig.getRealm()
                + "/protocol/openid-connect/logout";
    }

    private TokenResponse toTokenResponse(KeycloakTokenResponse response) {
        return TokenResponse.builder()
                .accessToken(response.getAccessToken())
                .refreshToken(response.getRefreshToken())
                .expiresIn(response.getExpiresIn())
                .tokenType(response.getTokenType())
                .build();
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeycloakTokenResponse {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Long expiresIn;

        @JsonProperty("token_type")
        private String tokenType;
    }
}
