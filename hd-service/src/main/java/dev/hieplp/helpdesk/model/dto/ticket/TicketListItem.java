package dev.hieplp.helpdesk.model.dto.ticket;

import dev.hieplp.helpdesk.model.entity.Ticket;
import dev.hieplp.helpdesk.model.enums.TicketCategory;
import dev.hieplp.helpdesk.model.enums.TicketPriority;
import dev.hieplp.helpdesk.model.enums.TicketStatus;

import java.time.Instant;

public record TicketListItem(
        Long id,
        String title,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        Long requesterId,
        String requesterName,
        Long assigneeId,
        Instant createdAt,
        Instant updatedAt
) {

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
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
