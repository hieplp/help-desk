package dev.hieplp.helpdesk.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;
import java.util.Optional;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<TicketStatus> fromJson(String value) {
        try {
            return Optional.of(valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
