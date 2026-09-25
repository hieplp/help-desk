package dev.hieplp.helpdesk.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Account role: {@code requester} files tickets, {@code agent} works them.
 * Serializes lowercase on the wire.
 */
public enum Role {
    /** Files tickets; sees only their own. */
    REQUESTER,
    /** Works tickets; sees all, can be assigned. */
    AGENT;

    /**
     * Lowercase wire value, e.g. {@code "agent"}.
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
     * @param value e.g. {@code "requester"}
     * @return matching constant
     * @throws IllegalArgumentException on unknown value
     */
    public static Role fromJson(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
