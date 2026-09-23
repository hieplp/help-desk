package dev.hieplp.helpdesk.model.dto;

import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

public record TokenResponse(
        String value,
        Instant expiresAt
) {
    public static TokenResponse from(Jwt jwt) {
        return new TokenResponse(
                jwt.getTokenValue(),
                jwt.getExpiresAt()
        );
    }
}
