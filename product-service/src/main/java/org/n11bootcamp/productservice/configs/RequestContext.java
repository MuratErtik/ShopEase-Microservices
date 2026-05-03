package org.n11bootcamp.productservice.configs;

import jakarta.servlet.http.HttpServletRequest;
import org.n11bootcamp.productservice.exceptions.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class RequestContext {

    private static final String USER_ID_HEADER    = "X-User-ID";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLES_HEADER = "X-User-Roles";
    private static final String USER_NAME_HEADER    = "X-User-Name";
    private static final String USER_SURNAME_HEADER = "X-User-Surname";

    public UUID getCurrentUserId(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("User ID not found in request");
        }
        return UUID.fromString(userId);
    }

    public String getCurrentEmail(HttpServletRequest request) {
        String email = request.getHeader(USER_EMAIL_HEADER);
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("User email not found in request");
        }
        return email;
    }

    public List<String> getCurrentUserRoles(HttpServletRequest request) {
        String roles = request.getHeader(USER_ROLES_HEADER);
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        return Arrays.asList(roles.split(","));
    }

    public boolean isSeller(HttpServletRequest request) {
        return getCurrentUserRoles(request).contains("ROLE_SELLER");
    }

    public String getCurrentName(HttpServletRequest request) {
        String name = request.getHeader(USER_NAME_HEADER);
        return (name != null && !name.isBlank()) ? name : "";
    }

    public String getCurrentSurname(HttpServletRequest request) {
        String surname = request.getHeader(USER_SURNAME_HEADER);
        return (surname != null && !surname.isBlank()) ? surname : "";
    }

    public String getCurrentFullName(HttpServletRequest request) {
        return (getCurrentName(request) + " " + getCurrentSurname(request)).trim();
    }
}