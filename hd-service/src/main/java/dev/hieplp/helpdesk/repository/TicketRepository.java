package dev.hieplp.helpdesk.repository;

import dev.hieplp.helpdesk.model.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
