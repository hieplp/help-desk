package dev.hieplp.helpdesk.model.dto.ticket;

import dev.hieplp.helpdesk.model.entity.Ticket;
import dev.hieplp.helpdesk.model.enums.TicketCategory;
import dev.hieplp.helpdesk.model.enums.TicketPriority;
import dev.hieplp.helpdesk.model.enums.TicketStatus;

import java.time.Instant;
import java.util.List;

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
                comments
        );
    }
}
