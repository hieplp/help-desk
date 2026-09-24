package dev.hieplp.order.calculator.csv;

import dev.hieplp.order.calculator.error.OrderProcessingException;
import dev.hieplp.order.calculator.spi.TabularRow;
import dev.hieplp.order.calculator.spi.TabularSource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * {@link TabularSource} backed by Apache Commons CSV. Single-use; rows stream
 * lazily via {@link CSVParser}.
 */
final class CsvTabularSource implements TabularSource {

    private final Path path;
    private final Charset charset;
    private final Reader reader;
    private final CSVFormat format;

    private CSVParser parser;

    CsvTabularSource(Path path, Charset charset, CSVFormat format) {
        this.path = Objects.requireNonNull(path, "path");
        this.charset = Objects.requireNonNull(charset, "charset");
        this.format = Objects.requireNonNull(format, "format");
        this.reader = null;
    }

    CsvTabularSource(Reader reader, CSVFormat format) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.format = Objects.requireNonNull(format, "format");
        this.path = null;
        this.charset = null;
    }

    @SuppressWarnings("resource") // parser is owned by this source; closed in close()
    private CSVParser parser() {
        if (parser == null) {
            try {
                parser = path != null
                        ? CSVParser.parse(path, charset, format)
                        : format.parse(reader);
            } catch (IOException e) {
                throw new OrderProcessingException("failed to open csv input", e);
            }
        }
        return parser;
    }

    @Override
    public List<String> header() {
        return List.copyOf(parser().getHeaderNames());
    }

    @Override
    public Stream<TabularRow> rows() {
        return parser().stream().map(CsvTabularRow::new);
    }

    @Override
    public void close() {
        try {
            if (parser != null) {
                parser.close();
            } else if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            throw new OrderProcessingException("failed to close csv input", e);
        }
    }

    /**
     * {@code record.get(column)} throws on unknown column or field-count
     * mismatch; {@link BigDecimal} throws on non-numeric — the engine converts
     * both into {@code RowError}s per the {@code ErrorStrategy}.
     */
    private record CsvTabularRow(CSVRecord record) implements TabularRow {

        @Override
        public String text(String column) {
            return record.get(column);
        }

        @Override
        public BigDecimal number(String column) {
            return new BigDecimal(record.get(column));
        }

        @Override
        public long lineNumber() {
            return record.getRecordNumber();
        }

    }
}
