package dev.hieplp.helpdesk.model.dto.ticket;

import dev.hieplp.helpdesk.model.enums.TicketCategory;
import dev.hieplp.helpdesk.model.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /tickets} body. Title (max 120) and description (max 4000) are required and trimmed.
 * The caller cannot set status, requester, or assignee here.
 *
 * @param title short summary, required, max 120
 * @param description full problem description, required, max 4000
 * @param category ticket category, required
 * @param priority ticket priority, required
 */
public record CreateTicketRequest(
    @NotBlank @Size(max = 120) String title,
    @NotBlank @Size(max = 4000) String description,
    @NotNull TicketCategory category,
    @NotNull TicketPriority priority) {

  /** Trims title and description before validation. */
  public CreateTicketRequest {
    title = title == null ? null : title.trim();
    description = description == null ? null : description.trim();
  }
}
