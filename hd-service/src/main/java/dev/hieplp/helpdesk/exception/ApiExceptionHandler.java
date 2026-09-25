package dev.hieplp.helpdesk.exception;

import dev.hieplp.helpdesk.model.dto.error.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps exceptions to the spec error body {@code {"code","message"}}.
 * {@link ApiException} keeps its own status; malformed input → 400, unknown
 * route → 404, wrong method → 405, anything else → 500.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * Business errors: respond with the status the exception carries.
     */
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> api(ApiException e) {
        return body(e.getStatus(), e.getMessage());
    }

    /**
     * Unreadable body, wrong media type, failed validation, or bad path/query
     * type → 400.
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            HttpMediaTypeNotSupportedException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ErrorResponse> badRequest(Exception e) {
        return body(HttpStatus.BAD_REQUEST, "Request is invalid");
    }

    /**
     * No route matched → 404.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> notFound(NoResourceFoundException e) {
        return body(HttpStatus.NOT_FOUND, "Not found");
    }

    /**
     * Route exists but the HTTP method does not → 405.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
    }

    /**
     * Catch-all → 500; logged server-side, generic message to the client.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> internal(Exception e) {
        log.error("Unhandled exception", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");
    }

    private ResponseEntity<ErrorResponse> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message));
    }
}
