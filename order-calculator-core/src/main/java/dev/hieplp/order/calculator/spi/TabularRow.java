package dev.hieplp.order.calculator.spi;

import dev.hieplp.order.calculator.error.ErrorStrategy;
import dev.hieplp.order.calculator.error.RowError;

import java.math.BigDecimal;

/**
 * One input row, accessed by header name.
 */
public interface TabularRow {

    /**
     * Display text of {@code column} — used for pass-through values.
     */
    String text(String column);

    /**
     * Numeric value of {@code column}. Implementations backed by typed formats
     * (Excel) must return the stored number, not the formatted text — parsing a
     * display string like {@code 1,234.56} would corrupt the value.
     * Implementations may throw on missing/non-numeric content; the engine
     * converts failures into {@link RowError}s per the {@link ErrorStrategy}.
     */
    BigDecimal number(String column);

    /**
     * 1-based line number (data rows) for {@link RowError} diagnostics.
     */
    long lineNumber();
}
