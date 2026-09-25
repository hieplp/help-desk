package dev.hieplp.helpdesk.model.dto.auth;

import dev.hieplp.helpdesk.model.dto.user.UserResponse;

/**
 * {@code POST /auth/login} 200 body: signed token + the caller's profile.
 *
 * @param token signed JWT + expiry
 * @param user the authenticated user's profile
 */
public record LoginResponse(TokenResponse token, UserResponse user) {}
