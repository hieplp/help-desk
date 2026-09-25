package dev.hieplp.helpdesk.model.dto.ticket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

/**
 * {@code PATCH /tickets/:id} body — only {@code status} and {@code assigneeId} are patchable.
 * Fields stay raw {@link JsonNode}s so validation order lives in the service: an absent key is a
 * {@code null} field (untouched), {@code null} in JSON is a {@code NullNode} (unassign), and any
 * other value is validated against the rules in {@code docs/api-rules.md}.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = false)
public class PatchTicketRequest {

  private JsonNode status;

  private JsonNode assigneeId;

  /**
   * Whether at least one patchable key was sent. An empty body {@code {}} binds to all-null
   * fields → false → the service rejects with 400.
   */
  public boolean isEmpty() {
    return status == null && assigneeId == null;
  }
}
