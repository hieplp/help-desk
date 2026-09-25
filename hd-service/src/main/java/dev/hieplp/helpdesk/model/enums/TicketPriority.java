package dev.hieplp.helpdesk.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/**
 * Ticket priority: {@code low} | {@code medium} | {@code high}. Serializes lowercase on the wire.
 */
public enum TicketPriority {
  /** Can wait. */
  LOW,
  /** Normal urgency. */
  MEDIUM,
  /** Needs attention soon. */
  HIGH;

  /**
   * Lowercase wire value, e.g. {@code "high"}.
   *
   * @return JSON form
   */
  @JsonValue
  public String toJson() {
    return name().toLowerCase(Locale.ROOT);
  }
}
