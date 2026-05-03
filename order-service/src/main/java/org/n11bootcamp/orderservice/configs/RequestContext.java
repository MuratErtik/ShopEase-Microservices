package org.n11bootcamp.orderservice.configs;



import jakarta.servlet.http.HttpServletRequest;
import org.n11bootcamp.orderservice.exceptions.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RequestContext {

    private static final String USER_ID_HEADER = "X-User-ID";

    private static final String USER_EMAIL_HEADER = "X-User-Email";

    public UUID getCurrentUserId(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("User ID not found in request");
        }
        return UUID.fromString(userId);
    }

    public String getCurrentUserEmail(HttpServletRequest request) {
        String email = request.getHeader(USER_EMAIL_HEADER);
        if (email == null || email.isBlank()) {
            return "test@test.com";
        }
        return email;
    }
}
