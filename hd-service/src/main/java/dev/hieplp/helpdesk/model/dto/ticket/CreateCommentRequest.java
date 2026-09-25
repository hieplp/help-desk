package dev.hieplp.helpdesk.model.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /tickets/:id/comments} body. Required, trimmed, max 2000.
 *
 * @param body comment text
 */
public record CreateCommentRequest(@NotBlank @Size(max = 2000) String body) {

  /** Trims the body before validation. */
  public CreateCommentRequest {
    body = body == null ? null : body.trim();
  }
}
