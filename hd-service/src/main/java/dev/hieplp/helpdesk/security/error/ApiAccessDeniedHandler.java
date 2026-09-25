package dev.hieplp.helpdesk.security.error;

import dev.hieplp.helpdesk.model.dto.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes the spec error body for authenticated callers who lack the role or own the wrong ticket:
 * 403 {@code {"code":"forbidden","message":"Forbidden"}}.
 */
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  /** Responds 403 with the JSON error body. */
  @Override
  public void handle(
      @NonNull HttpServletRequest request,
      HttpServletResponse response,
      @NonNull AccessDeniedException ex)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(), ErrorResponse.of(HttpStatus.FORBIDDEN, "Forbidden"));
  }
}
