package dev.hieplp.helpdesk.repository;

import dev.hieplp.helpdesk.model.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Ticket persistence. {@link JpaSpecificationExecutor} backs the role/status
 * filters in the list endpoint.
 */
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
}
