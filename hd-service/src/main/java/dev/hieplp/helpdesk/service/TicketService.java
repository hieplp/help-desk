package dev.hieplp.helpdesk.service;

import dev.hieplp.helpdesk.model.dto.ticket.CreateTicketRequest;
import dev.hieplp.helpdesk.model.dto.ticket.TicketResponse;

public interface TicketService {

    TicketResponse create(Long requesterId, CreateTicketRequest request);

}
