package dev.hieplp.helpdesk.model.dto;

public record LoginResponse(
        TokenResponse token,
        UserResponse user
) {
}
