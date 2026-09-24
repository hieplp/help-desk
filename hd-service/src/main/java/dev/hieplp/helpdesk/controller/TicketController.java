package dev.hieplp.helpdesk.controller;

import dev.hieplp.helpdesk.model.dto.ticket.CreateTicketRequest;
import dev.hieplp.helpdesk.model.dto.ticket.TicketDetail;
import dev.hieplp.helpdesk.model.dto.ticket.TicketListItem;
import dev.hieplp.helpdesk.model.dto.ticket.TicketResponse;
import dev.hieplp.helpdesk.security.Caller;
import dev.hieplp.helpdesk.security.CurrentCaller;
import dev.hieplp.helpdesk.security.CurrentUser;
import dev.hieplp.helpdesk.service.TicketService;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping(path = "/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> create(
            @CurrentUser Long userId,
            @Valid @RequestBody CreateTicketRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(userId, request));
    }

    @GetMapping
    public List<TicketListItem> list(
            @CurrentCaller Caller caller,
            @RequestParam(required = false) String status
    ) {
        return ticketService.list(caller, status);
    }

    @GetMapping("/{id}")
    public TicketDetail get(
            @CurrentCaller Caller caller,
            @PathVariable Long id
    ) {
        return ticketService.get(caller, id);
    }
}
