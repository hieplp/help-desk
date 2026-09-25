package dev.hieplp.helpdesk.model.dto.ticket;

import dev.hieplp.helpdesk.model.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

/**
 * {@code PATCH /tickets/:id} body — the only patchable field is {@code status}. Unknown values or
 * fields fail binding → 400; a null or missing status fails {@link NotNull} → 400.
 *
 * @param status target lifecycle status
 */
public record UpdateStatusRequest(@NotNull TicketStatus status) {}
