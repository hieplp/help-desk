package dev.hieplp.helpdesk.model.dto.ticket;

import dev.hieplp.helpdesk.model.entity.Comment;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long ticketId,
        Long authorId,
        String body,
        Instant createdAt
) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                comment.getAuthor().getId(),
                comment.getBody(),
                comment.getCreatedAt()
        );
    }
}
