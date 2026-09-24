package dev.hieplp.order.calculator.error;

/**
 * Single unchecked exception type for schema errors, row errors
 * ({@link ErrorStrategy#FAIL_FAST}), and wrapped IO failures.
 */
public class OrderProcessingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final RowError rowError;

    public OrderProcessingException(String message) {
        this(message, null, null);
    }

    public OrderProcessingException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public OrderProcessingException(RowError rowError) {
        this(rowError.message(), null, rowError);
    }

    private OrderProcessingException(String message, Throwable cause, RowError rowError) {
        super(message, cause);
        this.rowError = rowError;
    }

    /**
     * The offending row, if this exception was raised for a row error.
     */
    public RowError rowError() {
        return rowError;
    }

}
