package dev.hieplp.helpdesk.service;
import dev.hieplp.helpdesk.model.dto.ticket.CommentResponse;
import dev.hieplp.helpdesk.model.dto.ticket.CreateCommentRequest;
import dev.hieplp.helpdesk.model.dto.ticket.CreateTicketRequest;
import dev.hieplp.helpdesk.model.dto.ticket.TicketDetail;
import dev.hieplp.helpdesk.model.dto.ticket.TicketListItem;
import dev.hieplp.helpdesk.model.dto.ticket.TicketResponse;
import dev.hieplp.helpdesk.model.dto.ticket.UpdateAssigneeRequest;
import dev.hieplp.helpdesk.model.dto.ticket.UpdateStatusRequest;
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
   * Changes a ticket's status. Agents may set {@code in_progress}, {@code resolved}, or {@code
   * closed} from any non-closed status; requesters may only set {@code closed} on their own
   * ticket. {@code closed} is terminal.
   *
   * @param caller authenticated user
   * @param ticketId ticket id
   * @param request target status
   * @return the updated ticket, without comments
   * @throws dev.hieplp.helpdesk.exception.ApiException 400 on bad id; 404 when missing or owned
   *     by another requester; 403 when the caller's role cannot make the transition or the ticket
   *     is closed
   */
  TicketResponse updateStatus(Caller caller, Long ticketId, UpdateStatusRequest request);

  /**
   * Assigns a ticket to an agent, or unassigns when {@code assigneeId} is null. Agent-only.
   *
   * @param caller authenticated user
   * @param ticketId ticket id
   * @param request assignee id or null
   * @return the updated ticket, without comments
   * @throws dev.hieplp.helpdesk.exception.ApiException 400 on bad id, missing key, or non-agent
   *     assignee; 404 when missing or owned by another requester; 403 for requesters or a closed
   *     ticket
   */
  TicketResponse updateAssignee(Caller caller, Long ticketId, UpdateAssigneeRequest request);

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
