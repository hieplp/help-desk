package dev.hieplp.helpdesk.service;

import dev.hieplp.helpdesk.model.dto.ticket.CreateTicketRequest;
import dev.hieplp.helpdesk.model.dto.ticket.TicketDetail;
import dev.hieplp.helpdesk.model.dto.ticket.TicketListItem;
import dev.hieplp.helpdesk.model.dto.ticket.TicketResponse;
import dev.hieplp.helpdesk.security.principal.Caller;

import java.util.List;

public interface TicketService {

    TicketResponse create(Caller caller, CreateTicketRequest request);

    List<TicketListItem> list(Caller caller, String statusParam);

    TicketDetail get(Caller caller, Long ticketId);

}
