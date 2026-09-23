package dev.hieplp.helpdesk.model.dto;

import dev.hieplp.helpdesk.common.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record LoginRequest(
        @NotBlank @Size(max = 254) @Pattern(regexp = "^[^@]+@[^@]+\\.[^@]*$") String email,
        @NotBlank @MaxBytes(72) String password
) {

    public LoginRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        password = password == null ? null : password.trim();
    }
}
