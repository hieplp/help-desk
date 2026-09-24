package dev.hieplp.helpdesk.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH;

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
