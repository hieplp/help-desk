package dev.hieplp.helpdesk.model.dto.ticket;

import dev.hieplp.helpdesk.model.enums.TicketCategory;
import dev.hieplp.helpdesk.model.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull TicketCategory category,
        @NotNull TicketPriority priority
) {

    public CreateTicketRequest {
        title = title == null ? null : title.trim();
        description = description == null ? null : description.trim();
    }

}
