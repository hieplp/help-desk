package dev.hieplp.order.calculator.spi;

import dev.hieplp.order.calculator.error.OrderProcessingException;
import dev.hieplp.order.calculator.model.CellValue;
import dev.hieplp.order.calculator.model.OutputColumn;

import java.util.List;

/**
 * A writable tabular output. Implementations are format-specific, single-use,
 * and not thread-safe. IO failures are reported as unchecked
 * {@link OrderProcessingException}.
 */
public interface TabularSink extends AutoCloseable {

    /**
     * Writes the output header row, in {@link OutputColumn} order.
     */
    void writeHeader(List<String> columns);

    /**
     * Writes one row. {@link CellValue.Number} cells must stay numeric in
     * formats that support it (Excel); text formats render the plain string.
     */
    void writeRow(List<CellValue> cells);

    @Override
    void close();
}
