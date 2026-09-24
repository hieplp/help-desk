package dev.hieplp.order.calculator.csv;

import dev.hieplp.order.calculator.spi.TabularSink;
import dev.hieplp.order.calculator.spi.TabularSource;
import org.apache.commons.csv.CSVFormat;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Factory for CSV-backed {@link TabularSource} / {@link TabularSink}.
 */
public final class Csv {

    /**
     * Header row, comma delimiter, double-quote quoting, trimmed cells.
     */
    public static final CSVFormat DEFAULT_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .get();

    private Csv() {
    }

    public static TabularSource source(Path path) {
        return source(path, StandardCharsets.UTF_8);
    }

    public static TabularSource source(Path path, Charset charset) {
        return source(path, charset, DEFAULT_FORMAT);
    }

    public static TabularSource source(Path path, Charset charset, CSVFormat format) {
        return new CsvTabularSource(path, charset, format);
    }

    public static TabularSource source(Reader reader) {
        return source(reader, DEFAULT_FORMAT);
    }

    public static TabularSource source(Reader reader, CSVFormat format) {
        return new CsvTabularSource(reader, format);
    }

    public static TabularSink sink(Path path) {
        return sink(path, StandardCharsets.UTF_8);
    }

    public static TabularSink sink(Path path, Charset charset) {
        return sink(path, charset, DEFAULT_FORMAT);
    }

    public static TabularSink sink(Path path, Charset charset, CSVFormat format) {
        return new CsvTabularSink(path, charset, format);
    }

    public static TabularSink sink(Writer writer) {
        return sink(writer, DEFAULT_FORMAT);
    }

    public static TabularSink sink(Writer writer, CSVFormat format) {
        return new CsvTabularSink(writer, format);
    }
}
