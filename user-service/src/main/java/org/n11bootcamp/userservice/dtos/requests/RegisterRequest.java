package org.n11bootcamp.userservice.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.n11bootcamp.userservice.entities.User;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "First name cannot be blank")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name cannot be blank")
    @Size(max = 100)
    private String lastName;

    @NotNull(message = "Role cannot be null")
    private User.Role role;
}
