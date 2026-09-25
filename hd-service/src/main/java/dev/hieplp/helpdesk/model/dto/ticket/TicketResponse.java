package dev.hieplp.helpdesk.model.dto.ticket;

import dev.hieplp.helpdesk.model.entity.Ticket;
import dev.hieplp.helpdesk.model.enums.TicketCategory;
import dev.hieplp.helpdesk.model.enums.TicketPriority;
import dev.hieplp.helpdesk.model.enums.TicketStatus;

import java.time.Instant;

/**
 * Single-ticket response for create/update — full fields, no comments.
 *
 * @param id ticket id
 * @param title short summary
 * @param description full problem description
 * @param category ticket category
 * @param priority ticket priority
 * @param status lifecycle status
 * @param requesterId creator's user id
 * @param assigneeId assigned agent's id, null when unassigned
 * @param createdAt when the ticket was filed
 * @param updatedAt last change
 */
public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        Long requesterId,
        Long assigneeId,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Maps a {@link Ticket} entity to the response shape.
     *
     * @param ticket entity
     * @return ticket; {@code assigneeId} is null when unassigned
     */
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getRequester().getId(),
                ticket.getAssignee() == null ? null : ticket.getAssignee().getId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
