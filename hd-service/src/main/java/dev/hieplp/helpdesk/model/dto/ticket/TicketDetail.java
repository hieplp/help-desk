package dev.hieplp.helpdesk.model.dto.ticket;

import dev.hieplp.helpdesk.model.entity.Ticket;
import dev.hieplp.helpdesk.model.enums.TicketCategory;
import dev.hieplp.helpdesk.model.enums.TicketPriority;
import dev.hieplp.helpdesk.model.enums.TicketStatus;
import java.time.Instant;
import java.util.List;

/**
 * {@code GET /tickets/:id} 200 body: full ticket including description and comments (oldest first).
 *
 * @param id ticket id
 * @param title short summary
 * @param description full problem description
 * @param category ticket category
 * @param priority ticket priority
 * @param status lifecycle status
 * @param requesterId creator's user id
 * @param requesterName creator's display name
 * @param assigneeId assigned agent's id, null when unassigned
 * @param createdAt when the ticket was filed
 * @param updatedAt last change
 * @param comments comments oldest first
 */
public record TicketDetail(
    Long id,
    String title,
    String description,
    TicketCategory category,
    TicketPriority priority,
    TicketStatus status,
    Long requesterId,
    String requesterName,
    Long assigneeId,
    Instant createdAt,
    Instant updatedAt,
    List<CommentResponse> comments
) {

  /**
   * Maps a {@link Ticket} entity plus its comments to the detail shape.
   *
   * @param ticket entity
   * @param comments comments oldest first
   * @return ticket detail; {@code assigneeId} is null when unassigned
   */
  public static TicketDetail from(Ticket ticket, List<CommentResponse> comments) {
    return new TicketDetail(
        ticket.getId(),
        ticket.getTitle(),
        ticket.getDescription(),
        ticket.getCategory(),
        ticket.getPriority(),
        ticket.getStatus(),
        ticket.getRequester().getId(),
        ticket.getRequester().getName(),
        ticket.getAssignee() == null ? null : ticket.getAssignee().getId(),
        ticket.getCreatedAt(),
        ticket.getUpdatedAt(),
        comments);
  }
}
