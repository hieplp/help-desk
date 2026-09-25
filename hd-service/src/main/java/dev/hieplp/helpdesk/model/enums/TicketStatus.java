package dev.hieplp.helpdesk.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;
import java.util.Optional;

/**
 * Ticket lifecycle: {@code open} → {@code in_progress} → {@code resolved} →
 * {@code closed}. {@code closed} is terminal — no reopen.
 * Serializes lowercase on the wire.
 */
public enum TicketStatus {
    /** Newly filed, not yet picked up. */
    OPEN,
    /** An agent is working it. */
    IN_PROGRESS,
    /** Fixed, awaiting confirmation. */
    RESOLVED,
    /** Done. Terminal — no reopen. */
    CLOSED;

    /**
     * Lowercase wire value, e.g. {@code "in_progress"}.
     *
     * @return JSON form
     */
    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Parses a wire value back to the enum, case-insensitive.
     *
     * @param value e.g. {@code "open"}
     * @return matching constant, or empty on unknown/null value
     */
    public static Optional<TicketStatus> fromJson(String value) {
        try {
            return Optional.of(valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
