package dev.hieplp.helpdesk.service;

import dev.hieplp.helpdesk.model.dto.ticket.CreateTicketRequest;
import dev.hieplp.helpdesk.model.dto.ticket.TicketDetail;
import dev.hieplp.helpdesk.model.dto.ticket.TicketListItem;
import dev.hieplp.helpdesk.model.dto.ticket.TicketResponse;
import dev.hieplp.helpdesk.security.principal.Caller;
import java.util.List;

/** Ticket use cases. Role and ownership rules are enforced here, not in the controller. */
public interface TicketService {

  /**
   * Creates a ticket owned by the caller.
   *
   * @param caller authenticated user; becomes the requester
   * @param request title, description, category, priority
   * @return the created ticket, status {@code open}, assignee {@code null}
   */
  TicketResponse create(Caller caller, CreateTicketRequest request);

  /**
   * Lists tickets visible to the caller, newest {@code updatedAt} first.
   *
   * @param caller authenticated user; requesters are limited to their own tickets
   * @param statusParam optional status filter
   * @return list items without description
   * @throws dev.hieplp.helpdesk.exception.ApiException 400 on unknown status value
   */
  List<TicketListItem> list(Caller caller, String statusParam);

  /**
   * Returns one ticket with comments, oldest first.
   *
   * @param caller authenticated user
   * @param ticketId ticket id
   * @return ticket detail
   * @throws dev.hieplp.helpdesk.exception.ApiException 400 on non-positive id, 404 when missing or
   *         owned by another requester
   */
  TicketDetail get(Caller caller, Long ticketId);
}
