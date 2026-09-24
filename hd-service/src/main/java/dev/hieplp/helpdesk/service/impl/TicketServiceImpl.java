package dev.hieplp.helpdesk.service.impl;

import dev.hieplp.helpdesk.exception.ApiException;
import dev.hieplp.helpdesk.model.dto.ticket.CommentResponse;
import dev.hieplp.helpdesk.model.dto.ticket.CreateTicketRequest;
import dev.hieplp.helpdesk.model.dto.ticket.TicketDetail;
import dev.hieplp.helpdesk.model.dto.ticket.TicketListItem;
import dev.hieplp.helpdesk.model.dto.ticket.TicketResponse;
import dev.hieplp.helpdesk.model.entity.Ticket;
import dev.hieplp.helpdesk.model.enums.Role;
import dev.hieplp.helpdesk.model.enums.TicketStatus;
import dev.hieplp.helpdesk.repository.CommentRepository;
import dev.hieplp.helpdesk.repository.TicketRepository;
import dev.hieplp.helpdesk.repository.UserRepository;
import dev.hieplp.helpdesk.security.Caller;
import dev.hieplp.helpdesk.service.TicketService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    @Override
    public TicketResponse create(Long requesterId, CreateTicketRequest request) {
        log.info(
                "Creating ticket for requesterId={} category={} priority={}",
                requesterId, request.category(), request.priority()
        );

        var requester = userRepository.getReferenceById(requesterId);
        var ticket = Ticket.builder()
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .priority(request.priority())
                .status(TicketStatus.OPEN)
                .requester(requester)
                .build();
        var saved = ticketRepository.save(ticket);
        log.info("Created ticket id={} for requesterId={}", saved.getId(), requesterId);

        return TicketResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketListItem> list(Caller caller, String statusParam) {
        log.info(
                "Listing tickets for callerId={} role={} status={}",
                caller.id(), caller.role(), statusParam
        );

        TicketStatus status = null;
        if (statusParam != null) {
            status = TicketStatus.fromJson(statusParam)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown status"));
        }

        TicketStatus filter = status;
        Specification<Ticket> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (caller.role() == Role.REQUESTER) {
                predicates.add(cb.equal(root.get("requester").get("id"), caller.id()));
            }
            if (filter != null) {
                predicates.add(cb.equal(root.get("status"), filter));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        var result = ticketRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                .map(TicketListItem::from)
                .toList();
        log.info("Returning {} tickets for callerId={}", result.size(), caller.id());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDetail get(Caller caller, Long ticketId) {
        log.info("Getting ticket id={} for callerId={} role={}", ticketId, caller.id(), caller.role());

        if (ticketId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid ticket id");
        }

        var ticket = ticketRepository.findById(ticketId)
                .filter(t -> caller.role() == Role.AGENT || caller.id().equals(t.getRequester().getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ticket not found"));

        var comments = commentRepository.findByTicketIdOrderByIdAsc(ticketId).stream()
                .map(CommentResponse::from)
                .toList();

        return TicketDetail.from(ticket, comments);
    }
}
