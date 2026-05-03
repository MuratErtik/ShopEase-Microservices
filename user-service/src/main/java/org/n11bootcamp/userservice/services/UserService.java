package org.n11bootcamp.userservice.services;




import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.userservice.dtos.repsonses.TokenResponse;
import org.n11bootcamp.userservice.dtos.repsonses.UserResponse;
import org.n11bootcamp.userservice.dtos.requests.*;
import org.n11bootcamp.userservice.entities.User;
import org.n11bootcamp.userservice.exceptions.UserAlreadyExistsException;
import org.n11bootcamp.userservice.exceptions.UserNotFoundException;
import org.n11bootcamp.userservice.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;



    public UserResponse register(RegisterRequest request) {


        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "User already exists: " + request.getEmail()
            );
        }


        UUID keycloakId = keycloakService.createKeycloakUser(request);

        User user = User.builder()
                .id(keycloakId)
                .keycloakId(keycloakId)
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully. id: {}", savedUser.getId());

        return toResponse(savedUser);
    }



    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        return keycloakService.login(request);
    }



    @Transactional(readOnly = true)
    public TokenResponse refreshToken(String refreshToken) {
        return keycloakService.refreshToken(refreshToken);
    }



    public void logout(String refreshToken) {
        keycloakService.logout(refreshToken);
        log.info("User logged out successfully");
    }



    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found: " + id
                ));
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(UUID userId) {
        return getUserById(userId);
    }



    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
