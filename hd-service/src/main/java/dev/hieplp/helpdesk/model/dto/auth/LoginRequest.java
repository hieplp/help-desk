package dev.hieplp.helpdesk.model.dto.auth;

import dev.hieplp.helpdesk.common.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * {@code POST /auth/login} body. Email is trimmed and lowercased, password trimmed;
 * password is capped at 72 UTF-8 bytes (bcrypt limit).
 *
 * @param email account email, required, valid shape, max 254
 * @param password account password, required, max 72 UTF-8 bytes
 */
public record LoginRequest(
        @NotBlank @Size(max = 254) @Pattern(regexp = "^[^@]+@[^@]+\\.[^@]*$") String email,
        @NotBlank @MaxBytes(72) String password
) {

    /**
     * Normalizes input: trims both fields and lowercases the email.
     */
    public LoginRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        password = password == null ? null : password.trim();
    }
}
