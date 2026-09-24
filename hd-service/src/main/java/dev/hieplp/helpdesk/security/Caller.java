package dev.hieplp.helpdesk.security;

import dev.hieplp.helpdesk.model.enums.Role;
import org.springframework.security.core.Authentication;

/**
 * The authenticated caller: user id (token subject) + role (token claim).
 * Built from the {@link Authentication} that {@link JwtAuthFilter} installs.
 */
public record Caller(Long id, Role role) {

    public static Caller from(Authentication authentication) {
        var id = (Long) authentication.getPrincipal();
        var authority = authentication.getAuthorities().iterator().next().getAuthority();
        return new Caller(id, Role.fromJson(authority));
    }
}
