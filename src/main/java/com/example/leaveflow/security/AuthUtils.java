package com.example.leaveflow.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Static utility for controller-level authorization checks.
 * Reads the identity set by {@link JwtFilter}.
 */
public final class AuthUtils {

    private AuthUtils() {}

    /**
     * Requires an authenticated employee (valid token).
     */
    public static AuthenticatedEmployee requireAuthenticated(HttpServletRequest request) {
        AuthenticatedEmployee auth = (AuthenticatedEmployee) request.getAttribute(JwtFilter.ATTR_AUTH_EMPLOYEE);
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        return auth;
    }

    /**
     * Requires the authenticated user to be an ADMIN.
     */
    public static AuthenticatedEmployee requireAdmin(HttpServletRequest request) {
        AuthenticatedEmployee auth = requireAuthenticated(request);
        if (!auth.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required.");
        }
        return auth;
    }

    /**
     * Requires the authenticated user to be either an ADMIN or the specific employee themselves.
     */
    public static AuthenticatedEmployee requireAdminOrSelf(HttpServletRequest request, Long targetEmployeeId) {
        AuthenticatedEmployee auth = requireAuthenticated(request);
        if (!auth.isAdmin() && !auth.id().equals(targetEmployeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.");
        }
        return auth;
    }
}
