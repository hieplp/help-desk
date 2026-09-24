package dev.hieplp.helpdesk.controller;

import dev.hieplp.helpdesk.model.dto.ticket.CreateTicketRequest;
import dev.hieplp.helpdesk.model.dto.ticket.TicketResponse;
import dev.hieplp.helpdesk.security.CurrentUser;
import dev.hieplp.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
