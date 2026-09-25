package dev.hieplp.helpdesk.controller;

import dev.hieplp.helpdesk.model.dto.ticket.CreateTicketRequest;
import dev.hieplp.helpdesk.model.dto.ticket.TicketDetail;
import dev.hieplp.helpdesk.model.dto.ticket.TicketListItem;
import dev.hieplp.helpdesk.model.dto.ticket.TicketResponse;
import dev.hieplp.helpdesk.security.principal.Caller;
import dev.hieplp.helpdesk.security.principal.CurrentCaller;
import dev.hieplp.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ticket endpoints under {@code /tickets}. Role and ownership rules live in {@link TicketService};
 * this layer only binds HTTP.
 */
@RestController
@RequestMapping(path = "/tickets")
@RequiredArgsConstructor
public class TicketController {

  private final TicketService ticketService;

  /**
   * Creates a ticket owned by the caller. Status starts {@code open}, assignee {@code null}; the
   * caller cannot set either here.
   *
   * @param caller authenticated user; becomes {@code requesterId}
   * @param request title, description, category, priority
   * @return 201 created ticket
   */
  @PostMapping
  public ResponseEntity<TicketResponse> create(
      @CurrentCaller Caller caller, @Valid @RequestBody CreateTicketRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(caller, request));
  }

  /**
   * Lists tickets visible to the caller: requesters see only their own, agents see all.
   *
   * @param caller authenticated user
   * @param status optional status filter; unknown value → 400
   * @return 200 list items (no description), newest {@code updatedAt} first
   */
  @GetMapping
  public List<TicketListItem> list(
      @CurrentCaller Caller caller, @RequestParam(required = false) String status
  ) {
    return ticketService.list(caller, status);
  }

  /**
   * Returns one ticket with its comments, oldest first.
   *
   * @param caller authenticated user
   * @param id ticket id
   * @return 200 ticket detail
   * @throws dev.hieplp.helpdesk.exception.ApiException 404 when the ticket is missing or belongs to
   *         another requester (existence is not confirmed)
   */
  @GetMapping("/{id}")
  public TicketDetail get(@CurrentCaller Caller caller, @PathVariable Long id) {
    return ticketService.get(caller, id);
  }
}
