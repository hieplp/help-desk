package dev.hieplp.helpdesk.exception;

import org.springframework.http.HttpStatus;

/**
 * Business error carrying an HTTP status. {@link ApiExceptionHandler} turns it
 * into the spec error body.
 */
public class ApiException extends RuntimeException {

    /** HTTP status to respond with. */
    private final HttpStatus status;

    /**
     * Creates the exception.
     *
     * @param status HTTP status to respond with
     * @param message error message sent to the client
     */
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Returns the response status.
     *
     * @return HTTP status for the response
     */
    public HttpStatus getStatus() {
        return status;
    }
}
