package org.n11bootcamp.userservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.userservice.dtos.repsonses.TokenResponse;
import org.n11bootcamp.userservice.dtos.repsonses.UserResponse;
import org.n11bootcamp.userservice.dtos.requests.LoginRequest;
import org.n11bootcamp.userservice.dtos.requests.RegisterRequest;
import org.n11bootcamp.userservice.entities.User;

import org.n11bootcamp.userservice.exceptions.UserAlreadyExistsException;
import org.n11bootcamp.userservice.exceptions.UserNotFoundException;
import org.n11bootcamp.userservice.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakService keycloakService;

    @InjectMocks
    private UserService userService;

    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password123";
    private static final String FIRST_NAME = "Test";
    private static final String LAST_NAME = "User";

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .role(User.Role.USER)
                .build();

        loginRequest = LoginRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build();
    }

    private User sampleUser(UUID id) {
        return User.builder()
                .id(id)
                .keycloakId(id)
                .email(EMAIL)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .role(User.Role.SELLER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("Should create user and return response when email is available")
        void register_createsUser_whenEmailAvailable() {
            UUID keycloakId = UUID.randomUUID();
            User savedUser = sampleUser(keycloakId);

            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(keycloakService.createKeycloakUser(registerRequest)).thenReturn(keycloakId);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            UserResponse response = userService.register(registerRequest);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(keycloakId);
            assertThat(response.getEmail()).isEqualTo(EMAIL);
            assertThat(response.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(response.getLastName()).isEqualTo(LAST_NAME);
            assertThat(response.getRole()).isEqualTo(User.Role.SELLER);
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when email is taken")
        void register_throwsUserAlreadyExists_whenEmailTaken() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> userService.register(registerRequest))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining(EMAIL);

            verify(keycloakService, never()).createKeycloakUser(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should persist user with keycloakId as both id and keycloakId")
        void register_persistsUser_withKeycloakIdAsId() {
            UUID keycloakId = UUID.randomUUID();

            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(keycloakService.createKeycloakUser(registerRequest)).thenReturn(keycloakId);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.register(registerRequest);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User captured = captor.getValue();
            assertThat(captured.getId()).isEqualTo(keycloakId);
            assertThat(captured.getKeycloakId()).isEqualTo(keycloakId);
            assertThat(captured.getEmail()).isEqualTo(EMAIL);
            assertThat(captured.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(captured.getLastName()).isEqualTo(LAST_NAME);
            assertThat(captured.getRole()).isEqualTo(User.Role.USER);
        }

        @Test
        @DisplayName("Should call keycloak service before saving user")
        void register_callsKeycloak_beforeSavingUser() {
            UUID keycloakId = UUID.randomUUID();

            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(keycloakService.createKeycloakUser(registerRequest)).thenReturn(keycloakId);
            when(userRepository.save(any(User.class))).thenReturn(sampleUser(keycloakId));

            userService.register(registerRequest);

            org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(keycloakService, userRepository);
            inOrder.verify(keycloakService).createKeycloakUser(registerRequest);
            inOrder.verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("Should delegate to keycloak service and return token response")
        void login_delegatesToKeycloakService() {
            TokenResponse expected = TokenResponse.builder()
                    .accessToken("access")
                    .refreshToken("refresh")
                    .expiresIn(300L)
                    .tokenType("Bearer")
                    .build();
            when(keycloakService.login(loginRequest)).thenReturn(expected);

            TokenResponse result = userService.login(loginRequest);

            assertThat(result).isSameAs(expected);
            verify(keycloakService).login(loginRequest);
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should delegate to keycloak service and return new token")
        void refreshToken_delegatesToKeycloakService() {
            TokenResponse expected = TokenResponse.builder()
                    .accessToken("new-access")
                    .refreshToken("new-refresh")
                    .build();
            when(keycloakService.refreshToken("old-refresh")).thenReturn(expected);

            TokenResponse result = userService.refreshToken("old-refresh");

            assertThat(result).isSameAs(expected);
            verify(keycloakService).refreshToken("old-refresh");
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("Should delegate to keycloak service")
        void logout_delegatesToKeycloakService() {
            userService.logout("refresh-token");

            verify(keycloakService).logout("refresh-token");
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user response when user exists")
        void getUserById_returnsResponse_whenExists() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.of(sampleUser(id)));

            UserResponse response = userService.getUserById(id);

            assertThat(response.getId()).isEqualTo(id);
            assertThat(response.getEmail()).isEqualTo(EMAIL);
            assertThat(response.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(response.getLastName()).isEqualTo(LAST_NAME);
            assertThat(response.getRole()).isEqualTo(User.Role.SELLER);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void getUserById_throwsNotFound_whenMissing() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(id))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }

        @Test
        @DisplayName("Should map createdAt timestamp into response")
        void getUserById_mapsCreatedAt_intoResponse() {
            UUID id = UUID.randomUUID();
            User user = sampleUser(id);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            UserResponse response = userService.getUserById(id);

            assertThat(response.getCreatedAt()).isEqualTo(user.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("getMe")
    class GetMeTests {

        @Test
        @DisplayName("Should delegate to getUserById and return response")
        void getMe_delegatesToGetUserById() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.of(sampleUser(id)));

            UserResponse response = userService.getMe(id);

            assertThat(response.getId()).isEqualTo(id);
            verify(userRepository).findById(id);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when current user does not exist")
        void getMe_throwsNotFound_whenMissing() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getMe(id))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}