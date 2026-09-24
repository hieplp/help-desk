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
import java.util.regex.Pattern;
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
        int fieldCount = parser().getHeaderNames().size();
        return parser().stream().map(r -> new CsvTabularRow(r, fieldCount));
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
     * mismatch; plain-decimal check throws on non-numeric text — the engine
     * converts both into {@code RowError}s per the {@code ErrorStrategy}.
     */
    private record CsvTabularRow(CSVRecord record, int expectedFields) implements TabularRow {

        /**
         * Plain decimal notation only — no separators, exponents, or signs beyond a leading {@code -}.
         */
        private static final Pattern PLAIN_DECIMAL = Pattern.compile("-?(\\d+(\\.\\d+)?|\\.\\d+)");

        private void checkFieldCount() {
            if (record.size() != expectedFields) {
                throw new IllegalStateException(
                        "expected " + expectedFields + " fields but found " + record.size());
            }
        }

        @Override
        public String text(String column) {
            checkFieldCount();
            return record.get(column);
        }

        @Override
        public BigDecimal number(String column) {
            checkFieldCount();
            String raw = record.get(column);
            if (!PLAIN_DECIMAL.matcher(raw).matches()) {
                throw new NumberFormatException("not a plain decimal number: '" + raw + "'");
            }
            return new BigDecimal(raw);
        }

        @Override
        public long lineNumber() {
            return record.getRecordNumber();
        }

    }
}
