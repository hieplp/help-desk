package dev.hieplp.order.calculator.spi;

import dev.hieplp.order.calculator.error.OrderProcessingException;

import java.util.List;
import java.util.stream.Stream;

/**
 * A readable tabular input: a header plus a lazily-consumed stream of rows.
 * Implementations are format-specific, single-use, and not thread-safe.
 * IO failures are reported as unchecked {@link OrderProcessingException}.
 */
public interface TabularSource extends AutoCloseable {

    /**
     * Logical column names in input order.
     */
    List<String> header();

    /**
     * Data rows in input order; consumed once, lazily (streaming).
     */
    Stream<TabularRow> rows();

    @Override
    void close();
}
