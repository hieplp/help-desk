package dev.hieplp.order.calculator.error;

import java.io.Serializable;

/**
 * One row-level failure.
 *
 * @param lineNumber 1-based data-row number
 * @param column     offending column name, or {@code null} for whole-row issues
 * @param rawValue   offending raw cell content, or {@code null}
 * @param message    human-readable cause
 */
public record RowError(
        long lineNumber,
        String column,
        String rawValue,
        String message
) implements Serializable {

    private static final long serialVersionUID = 1L;

}
