package dev.hieplp.order.calculator.csv;

import dev.hieplp.order.calculator.error.OrderProcessingException;
import dev.hieplp.order.calculator.model.CellValue;
import dev.hieplp.order.calculator.spi.TabularSink;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * {@link TabularSink} backed by Apache Commons CSV. Renders every cell as text;
 * {@link CellValue.Number} uses {@code BigDecimal.toPlainString()}.
 */
final class CsvTabularSink implements TabularSink {

    private final Path path;
    private final Charset charset;
    private final Writer writer;
    private final CSVFormat format;

    private CSVPrinter printer;

    CsvTabularSink(Path path, Charset charset, CSVFormat format) {
        this.path = Objects.requireNonNull(path, "path");
        this.charset = Objects.requireNonNull(charset, "charset");
        this.format = Objects.requireNonNull(format, "format");
        this.writer = null;
    }

    CsvTabularSink(Writer writer, CSVFormat format) {
        this.writer = Objects.requireNonNull(writer, "writer");
        this.format = Objects.requireNonNull(format, "format");
        this.path = null;
        this.charset = null;
    }

    private static String render(CellValue cell) {
        return switch (cell) {
            case CellValue.Text t -> t.value();
            case CellValue.Number n -> n.value().toPlainString();
        };
    }

    @SuppressWarnings("resource") // printer is owned by this sink; closed in close()
    private CSVPrinter printer() {
        if (printer == null) {
            try {
                printer = path != null
                        ? new CSVPrinter(Files.newBufferedWriter(path, charset), format)
                        : new CSVPrinter(writer, format);
            } catch (IOException e) {
                throw new OrderProcessingException("failed to open csv output", e);
            }
        }
        return printer;
    }

    @Override
    public void writeHeader(List<String> columns) {
        try {
            printer().printRecord(columns);
        } catch (IOException e) {
            throw new OrderProcessingException("failed to write csv header", e);
        }
    }

    @Override
    public void writeRow(List<CellValue> cells) {
        try {
            printer().printRecord(cells.stream().map(CsvTabularSink::render).toList());
        } catch (IOException e) {
            throw new OrderProcessingException("failed to write csv row", e);
        }
    }

    @Override
    public void close() {
        try {
            if (printer != null) {
                printer.close();
            } else if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            throw new OrderProcessingException("failed to close csv output", e);
        }
    }
}
