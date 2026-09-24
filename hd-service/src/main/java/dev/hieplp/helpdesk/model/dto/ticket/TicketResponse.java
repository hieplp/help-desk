package dev.hieplp.helpdesk.model.dto.ticket;

import dev.hieplp.helpdesk.model.entity.Ticket;
import dev.hieplp.helpdesk.model.enums.TicketCategory;
import dev.hieplp.helpdesk.model.enums.TicketPriority;
import dev.hieplp.helpdesk.model.enums.TicketStatus;

import java.time.Instant;

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
