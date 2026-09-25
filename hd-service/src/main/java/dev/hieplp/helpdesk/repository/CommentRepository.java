package dev.hieplp.helpdesk.repository;

import dev.hieplp.helpdesk.model.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Comment persistence. */
public interface CommentRepository extends JpaRepository<Comment, Long> {

  /**
   * All comments on a ticket, oldest first.
   *
   * @param ticketId ticket id
   * @return comments ordered by id
   */
  List<Comment> findByTicketIdOrderByIdAsc(Long ticketId);
}
