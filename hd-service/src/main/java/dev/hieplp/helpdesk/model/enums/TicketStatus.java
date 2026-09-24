package dev.hieplp.helpdesk.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
