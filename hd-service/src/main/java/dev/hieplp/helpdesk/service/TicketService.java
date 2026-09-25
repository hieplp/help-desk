package dev.hieplp.helpdesk.service;
import dev.hieplp.helpdesk.model.dto.ticket.CommentResponse;
import dev.hieplp.helpdesk.model.dto.ticket.CreateCommentRequest;
import dev.hieplp.helpdesk.model.dto.ticket.CreateTicketRequest;
import dev.hieplp.helpdesk.model.dto.ticket.PatchTicketRequest;
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

  /**
   * Partially updates a ticket — only {@code status} and {@code assigneeId} are patchable, and
   * {@code assigneeId: null} unassigns while an absent key leaves it untouched.
   *
   * @param caller authenticated user
   * @param ticketId ticket id
   * @param patch parsed patch body; must contain at least one patchable key
   * @return the updated ticket, without comments
   * @throws dev.hieplp.helpdesk.exception.ApiException 400 on bad id, empty body, unknown field,
   *     bad enum, or non-agent assignee; 404 when missing or owned by another requester; 403 when
   *     the caller's role cannot make the change or the ticket is closed
   */
  TicketResponse update(Caller caller, Long ticketId, PatchTicketRequest patch);

  /**
   * Appends a comment to a ticket. Closed tickets still accept comments.
   *
   * @param caller authenticated user; becomes the author
   * @param ticketId ticket id
   * @param request comment body
   * @return the created comment
   * @throws dev.hieplp.helpdesk.exception.ApiException 400 on bad id, 404 when missing or owned by
   *     another requester
   */
  CommentResponse addComment(Caller caller, Long ticketId, CreateCommentRequest request);
}
