package dev.hieplp.helpdesk.model.dto.auth;

import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

/**
 * A signed JWT and its expiry instant.
 *
 * @param value encoded JWT, used as {@code Bearer <value>}
 * @param expiresAt when the token stops being accepted
 */
public record TokenResponse(
        String value,
        Instant expiresAt
) {

    /**
     * Builds the response from a decoded {@link Jwt}.
     *
     * @param jwt signed token
     * @return token value + {@code exp}
     */
    public static TokenResponse from(Jwt jwt) {
        return new TokenResponse(
                jwt.getTokenValue(),
                jwt.getExpiresAt()
        );
    }
}
