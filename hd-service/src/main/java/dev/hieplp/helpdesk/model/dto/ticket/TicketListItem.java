package dev.hieplp.helpdesk.model.dto.ticket;

import dev.hieplp.helpdesk.model.entity.Ticket;
import dev.hieplp.helpdesk.model.enums.TicketCategory;
import dev.hieplp.helpdesk.model.enums.TicketPriority;
import dev.hieplp.helpdesk.model.enums.TicketStatus;
import java.time.Instant;

/**
 * {@code GET /tickets} list item — same fields as the ticket minus description and comments.
 *
 * @param id ticket id
 * @param title short summary
 * @param category ticket category
 * @param priority ticket priority
 * @param status lifecycle status
 * @param requesterId creator's user id
 * @param requesterName creator's display name
 * @param assigneeId assigned agent's id, null when unassigned
 * @param assigneeName assigned agent's display name, null when unassigned
 * @param createdAt when the ticket was filed
 * @param updatedAt last change
 */
public record TicketListItem(
    Long id,
    String title,
    TicketCategory category,
    TicketPriority priority,
    TicketStatus status,
    Long requesterId,
    String requesterName,
    Long assigneeId,
    String assigneeName,
    Instant createdAt,
    Instant updatedAt
) {

  /**
   * Maps a {@link Ticket} entity to the list shape.
   *
   * @param ticket entity
   * @return list item; {@code assigneeId} is null when unassigned
   */
  public static TicketListItem from(Ticket ticket) {
    return new TicketListItem(
        ticket.getId(),
        ticket.getTitle(),
        ticket.getCategory(),
        ticket.getPriority(),
        ticket.getStatus(),
        ticket.getRequester().getId(),
        ticket.getRequester().getName(),
        ticket.getAssignee() == null ? null : ticket.getAssignee().getId(),
        ticket.getAssignee() == null ? null : ticket.getAssignee().getName(),
        ticket.getCreatedAt(),
        ticket.getUpdatedAt());
  }
}
