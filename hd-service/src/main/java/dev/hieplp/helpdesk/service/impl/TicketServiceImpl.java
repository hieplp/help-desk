package dev.hieplp.helpdesk.service.impl;

import dev.hieplp.helpdesk.exception.ApiException;
import dev.hieplp.helpdesk.model.dto.ticket.CommentResponse;
import dev.hieplp.helpdesk.model.dto.ticket.CreateCommentRequest;
import dev.hieplp.helpdesk.model.dto.ticket.CreateTicketRequest;
import dev.hieplp.helpdesk.model.dto.ticket.TicketDetail;
import dev.hieplp.helpdesk.model.dto.ticket.TicketListItem;
import dev.hieplp.helpdesk.model.dto.ticket.TicketResponse;
import dev.hieplp.helpdesk.model.dto.ticket.UpdateAssigneeRequest;
import dev.hieplp.helpdesk.model.dto.ticket.UpdateStatusRequest;
import dev.hieplp.helpdesk.model.entity.Comment;
import dev.hieplp.helpdesk.model.entity.Ticket;
import dev.hieplp.helpdesk.model.enums.Role;
import dev.hieplp.helpdesk.model.enums.TicketStatus;
import dev.hieplp.helpdesk.repository.CommentRepository;
import dev.hieplp.helpdesk.repository.TicketRepository;
import dev.hieplp.helpdesk.repository.UserRepository;
import dev.hieplp.helpdesk.security.principal.Caller;
import dev.hieplp.helpdesk.service.TicketService;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ticket business rules: requesters see and touch only their own tickets, agents see all. {@code
 * closed} is terminal — no reopen.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

  private final TicketRepository ticketRepository;
  private final UserRepository userRepository;
  private final CommentRepository commentRepository;

  /** {@inheritDoc} */
  @Override
  public TicketResponse create(Caller caller, CreateTicketRequest request) {
    log.info(
        "Creating ticket for requesterId={} category={} priority={}",
        caller.id(),
        request.category(),
        request.priority());

    var requester = userRepository.getReferenceById(caller.id());
    var ticket =
        Ticket.builder()
            .title(request.title())
            .description(request.description())
            .category(request.category())
            .priority(request.priority())
            .status(TicketStatus.OPEN)
            .requester(requester)
            .build();
    var saved = ticketRepository.save(ticket);
    log.info("Created ticket id={} for requesterId={}", saved.getId(), caller.id());

    return TicketResponse.from(saved);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public List<TicketListItem> list(Caller caller, String statusParam) {
    log.info(
        "Listing tickets for callerId={} role={} status={}",
        caller.id(),
        caller.role(),
        statusParam);

    TicketStatus status = null;
    if (statusParam != null) {
      status =
          TicketStatus.fromJson(statusParam)
              .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown status"));
    }

    TicketStatus filter = status;
    Specification<Ticket> spec =
        (root, query, cb) -> {
          var predicates = new ArrayList<Predicate>();
          if (caller.role() == Role.REQUESTER) {
            predicates.add(cb.equal(root.get("requester").get("id"), caller.id()));
          }
          if (filter != null) {
            predicates.add(cb.equal(root.get("status"), filter));
          }
          return cb.and(predicates.toArray(Predicate[]::new));
        };

    var result =
        ticketRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
            .map(TicketListItem::from)
            .toList();
    log.info("Returning {} tickets for callerId={}", result.size(), caller.id());
    return result;
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public TicketDetail get(Caller caller, Long ticketId) {
    log.info("Getting ticket id={} for callerId={} role={}", ticketId, caller.id(), caller.role());

    var ticket = loadVisible(caller, ticketId);

    var comments =
        commentRepository.findByTicketIdOrderByIdAsc(ticketId).stream()
            .map(CommentResponse::from)
            .toList();

    return TicketDetail.from(ticket, comments);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public TicketResponse updateStatus(Caller caller, Long ticketId, UpdateStatusRequest request) {
    log.info(
        "Updating status of ticket id={} for callerId={} role={}",
        ticketId,
        caller.id(),
        caller.role());

    var ticket = loadVisible(caller, ticketId);
    var status = request.status();

    // Role-level 403s — field errors already failed binding before this method ran.
    if (ticket.getStatus() == TicketStatus.CLOSED) {
      throw new ApiException(HttpStatus.FORBIDDEN, "Ticket is closed");
    }
    if (caller.role() == Role.REQUESTER) {
      if (status != TicketStatus.CLOSED) {
        throw new ApiException(HttpStatus.FORBIDDEN, "Requesters may only close their own ticket");
      }
    } else if (status == TicketStatus.OPEN) {
      throw new ApiException(HttpStatus.FORBIDDEN, "Agents cannot reopen a ticket to open");
    }

    ticket.setStatus(status);
    var saved = ticketRepository.save(ticket);
    log.info("Updated ticket id={} to status={}", saved.getId(), saved.getStatus());
    return TicketResponse.from(saved);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public TicketResponse updateAssignee(
      Caller caller, Long ticketId, UpdateAssigneeRequest request
  ) {
    log.info("Updating assignee of ticket id={} for callerId={}", ticketId, caller.id());

    var ticket = loadVisible(caller, ticketId);

    if (!request.isProvided()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "assigneeId is required");
    }
    Long assigneeId = request.getAssigneeId();
    if (assigneeId != null) {
      var assignee =
          userRepository
              .findById(assigneeId)
              .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown assignee"));
      if (assignee.getRole() != Role.AGENT) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "Assignee must be an agent");
      }
    }

    if (ticket.getStatus() == TicketStatus.CLOSED) {
      throw new ApiException(HttpStatus.FORBIDDEN, "Ticket is closed");
    }
    if (caller.role() == Role.REQUESTER) {
      throw new ApiException(HttpStatus.FORBIDDEN, "Only agents can assign tickets");
    }

    ticket.setAssignee(assigneeId == null ? null : userRepository.getReferenceById(assigneeId));
    var saved = ticketRepository.save(ticket);
    log.info(
        "Updated ticket id={} assigneeId={}",
        saved.getId(),
        saved.getAssignee() == null ? null : saved.getAssignee().getId());
    return TicketResponse.from(saved);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public CommentResponse addComment(Caller caller, Long ticketId, CreateCommentRequest request) {
    log.info("Adding comment to ticket id={} by callerId={}", ticketId, caller.id());

    var ticket = loadVisible(caller, ticketId);

    var comment = new Comment();
    comment.setTicket(ticket);
    comment.setAuthor(userRepository.getReferenceById(caller.id()));
    comment.setBody(request.body());
    var saved = commentRepository.save(comment);

    return CommentResponse.from(saved);
  }

  /**
   * Loads a ticket the caller may see: 400 on non-positive id, 404 when missing or owned by another
   * requester (existence is not confirmed).
   */
  private Ticket loadVisible(Caller caller, Long ticketId) {
    if (ticketId <= 0) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid ticket id");
    }
    return ticketRepository
        .findById(ticketId)
        .filter(t -> caller.role() == Role.AGENT || caller.id().equals(t.getRequester().getId()))
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ticket not found"));
  }
}
