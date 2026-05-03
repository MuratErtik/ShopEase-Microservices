package org.n11bootcamp.userservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.n11bootcamp.userservice.configs.RequestContext;
import org.n11bootcamp.userservice.dtos.repsonses.TokenResponse;
import org.n11bootcamp.userservice.dtos.repsonses.UserResponse;
import org.n11bootcamp.userservice.dtos.requests.LoginRequest;
import org.n11bootcamp.userservice.dtos.requests.RegisterRequest;
import org.n11bootcamp.userservice.services.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User management and authentication endpoints")
public class UserController {

    private final UserService userService;
    private final RequestContext requestContext;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "409", description = "User with provided credentials already exists")
    public ResponseEntity<UserResponse> register(
            @RequestBody @Valid RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue tokens")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid email or password")
    public ResponseEntity<TokenResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response) {

        TokenResponse tokens = userService.login(request);

        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.getRefreshToken()).toString());

        return ResponseEntity.ok(TokenResponse.builder()
                .accessToken(tokens.getAccessToken())
                .expiresIn(tokens.getExpiresIn())
                .tokenType(tokens.getTokenType())
                .build());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using the refresh token cookie")
    @ApiResponse(responseCode = "200", description = "New access token issued successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = "refresh_token") String refreshToken,
            HttpServletResponse response) {

        TokenResponse tokens = userService.refreshToken(refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.getRefreshToken()).toString());

        return ResponseEntity.ok(TokenResponse.builder()
                .accessToken(tokens.getAccessToken())
                .expiresIn(tokens.getExpiresIn())
                .tokenType(tokens.getTokenType())
                .build());
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate current session")
    @ApiResponse(responseCode = "204", description = "Logout successful and cookies cleared")
    @ApiResponse(responseCode = "401", description = "No active session found")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token") String refreshToken,
            HttpServletResponse response) {

        userService.logout(refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE, buildDeleteCookie().toString());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    @ApiResponse(responseCode = "200", description = "Profile details retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    public ResponseEntity<UserResponse> getMe(HttpServletRequest httpRequest) {
        UUID userId = requestContext.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(userService.getMe(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user profile by ID")
    @ApiResponse(responseCode = "200", description = "User profile found")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/users")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();
    }

    private ResponseCookie buildDeleteCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/users")
                .maxAge(Duration.ofSeconds(0))
                .build();
    }
}