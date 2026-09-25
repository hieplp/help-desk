package dev.hieplp.helpdesk.model.dto.ticket;

import dev.hieplp.helpdesk.model.entity.Comment;
import java.time.Instant;

/**
 * A ticket comment. Read-only via ticket detail — no edit, delete, or list endpoint.
 *
 * @param id comment id
 * @param ticketId owning ticket
 * @param authorId comment author
 * @param authorName comment author's display name
 * @param body comment text
 * @param createdAt when it was written
 */
public record CommentResponse(
    Long id, Long ticketId, Long authorId, String authorName, String body, Instant createdAt
) {

  /**
   * Maps a {@link Comment} entity to its API shape.
   *
   * @param comment entity
   * @return comment with ticket and author ids
   */
  public static CommentResponse from(Comment comment) {
    return new CommentResponse(
        comment.getId(),
        comment.getTicket().getId(),
        comment.getAuthor().getId(),
        comment.getAuthor().getName(),
        comment.getBody(),
        comment.getCreatedAt());
  }
}
