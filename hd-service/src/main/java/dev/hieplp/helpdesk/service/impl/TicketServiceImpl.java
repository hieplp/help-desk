package dev.hieplp.helpdesk.service.impl;

import dev.hieplp.helpdesk.model.dto.ticket.CreateTicketRequest;
import dev.hieplp.helpdesk.model.dto.ticket.TicketResponse;
import dev.hieplp.helpdesk.model.entity.Ticket;
import dev.hieplp.helpdesk.model.enums.TicketStatus;
import dev.hieplp.helpdesk.repository.TicketRepository;
import dev.hieplp.helpdesk.repository.UserRepository;
import dev.hieplp.helpdesk.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

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
}
