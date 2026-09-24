package dev.hieplp.order.calculator.error;

/**
 * Row-level error handling. Schema errors are always fatal.
 */
public enum ErrorStrategy {
    /**
     * Abort on the first bad row — {@link OrderProcessingException} carrying a {@link RowError}.
     */
    FAIL_FAST,
    /**
     * Skip the bad row, record a {@link RowError}, continue; totals cover good rows only.
     */
    SKIP_AND_COLLECT
}
