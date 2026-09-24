package dev.hieplp.helpdesk.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Role {
    REQUESTER,
    AGENT;

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Role fromJson(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
