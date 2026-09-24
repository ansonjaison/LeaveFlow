package com.example.leaveflow.security;

import com.example.leaveflow.enums.Role;

/**
 * Carries the identity of the authenticated employee for the duration of one HTTP request.
 *
 * Created by JwtFilter after a valid Supabase JWT is verified and the corresponding
 * employee record is loaded from the database.
 *
 * Stored as a request attribute so controllers can read it without
 * coupling to any HTTP framework in the service layer.
 *
 * Why a record?
 *   Records are immutable, concise, and ideal for value objects like this.
 *   They auto-generate equals, hashCode, and toString.
 */
public record AuthenticatedEmployee(Long id, String email, Role role) {

    /**
     * Convenience method: returns true if this employee has the ADMIN role.
     */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
