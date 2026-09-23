package dev.hieplp.helpdesk.model.dto;

import org.springframework.http.HttpStatus;

import java.util.Locale;

public record ErrorResponse(
        String code,
        String message
) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.name().toLowerCase(Locale.ROOT), message);
    }

}
