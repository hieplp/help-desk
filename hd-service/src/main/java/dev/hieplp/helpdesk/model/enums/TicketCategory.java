package dev.hieplp.helpdesk.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Ticket category: {@code hardware} | {@code software} | {@code access} | {@code other}.
 * Serializes lowercase on the wire.
 */
public enum TicketCategory {
    /** Physical devices. */
    HARDWARE,
    /** Applications and OS issues. */
    SOFTWARE,
    /** Accounts, permissions, credentials. */
    ACCESS,
    /** Anything not covered above. */
    OTHER;

    /**
     * Lowercase wire value, e.g. {@code "hardware"}.
     *
     * @return JSON form
     */
    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
