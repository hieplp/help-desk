package dev.hieplp.helpdesk.security.principal;

import dev.hieplp.helpdesk.model.enums.Role;
import dev.hieplp.helpdesk.security.jwt.JwtAuthFilter;
import org.springframework.security.core.Authentication;

/**
 * The authenticated caller: user id (token subject) + role (token claim). Built from the {@link
 * Authentication} that {@link JwtAuthFilter} installs.
 *
 * @param id user id from the token's {@code sub}
 * @param role role from the token's {@code role} claim
 */
public record Caller(Long id, Role role) {

  /**
   * Builds a caller from the authentication installed by {@link JwtAuthFilter}: principal = user
   * id, single authority = role.
   *
   * @param authentication current authentication
   * @return caller id + role
   */
  public static Caller from(Authentication authentication) {
    var id = (Long) authentication.getPrincipal();
    var authority = authentication.getAuthorities().iterator().next().getAuthority();
    return new Caller(id, Role.fromJson(authority));
  }
}
