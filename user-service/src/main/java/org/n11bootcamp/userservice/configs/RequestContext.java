package org.n11bootcamp.userservice.configs;




import jakarta.servlet.http.HttpServletRequest;
import org.n11bootcamp.userservice.exceptions.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RequestContext {

    private static final String USER_ID_HEADER = "X-User-ID";

    public UUID getCurrentUserId(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("User ID not found in request");
        }
        return UUID.fromString(userId);
    }
}
