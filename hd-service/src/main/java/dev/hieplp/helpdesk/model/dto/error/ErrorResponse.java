package dev.hieplp.helpdesk.model.dto.error;

import org.springframework.http.HttpStatus;

import java.util.Locale;

/**
 * The single error body shape: {@code {"code": "...", "message": "..."}}.
 * {@code code} is the lowercased HTTP status name.
 *
 * @param code lowercased status name, e.g. {@code "bad_request"}
 * @param message human-readable detail
 */
public record ErrorResponse(
        String code,
        String message
) {

    /**
     * Builds an error body from a status and message.
     *
     * @param status HTTP status; its lowercased name becomes {@code code}
     * @param message human-readable detail
     * @return error body
     */
    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.name().toLowerCase(Locale.ROOT), message);
    }

}
