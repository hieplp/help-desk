package dev.hieplp.order.calculator.excel;

import dev.hieplp.order.calculator.spi.TabularSink;
import dev.hieplp.order.calculator.spi.TabularSource;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Factory for {@code .xlsx}-backed {@link TabularSource} / {@link TabularSink}.
 */
public final class Excel {

    private Excel() {
    }

    public static TabularSource source(Path path) {
        return source(path, SourceConfig.defaults());
    }

    public static TabularSource source(Path path, SourceConfig config) {
        return new ExcelTabularSource(path, config);
    }

    public static TabularSource source(InputStream in) {
        return source(in, SourceConfig.defaults());
    }

    public static TabularSource source(InputStream in, SourceConfig config) {
        return new ExcelTabularSource(in, config);
    }

    public static TabularSink sink(Path path) {
        return sink(path, SinkConfig.defaults());
    }

    public static TabularSink sink(Path path, SinkConfig config) {
        return new ExcelTabularSink(path, config);
    }

    public static TabularSink sink(OutputStream out) {
        return sink(out, SinkConfig.defaults());
    }

    public static TabularSink sink(OutputStream out, SinkConfig config) {
        return new ExcelTabularSink(out, config);
    }

    /**
     * Read options for xlsx sources.
     *
     * @param sheetIndex      0-based sheet position (used when {@code sheetName} is null)
     * @param sheetName       sheet lookup by name; wins over {@code sheetIndex}
     * @param headerRowIndex  0-based index of the header row (rows above are skipped)
     * @param strictBlankRows false (default): first fully blank row ends the data
     *                        region; true: a blank row inside the data region is a
     *                        row error instead
     */
    public record SourceConfig(int sheetIndex, String sheetName, int headerRowIndex,
                               boolean strictBlankRows) {
        public SourceConfig {
            if (sheetIndex < 0) {
                throw new IllegalArgumentException("sheetIndex must be >= 0");
            }
            if (headerRowIndex < 0) {
                throw new IllegalArgumentException("headerRowIndex must be >= 0");
            }
        }

        public SourceConfig(int sheetIndex, String sheetName, int headerRowIndex) {
            this(sheetIndex, sheetName, headerRowIndex, false);
        }

        public static SourceConfig defaults() {
            return new SourceConfig(0, null, 0);
        }

        public SourceConfig sheetName(String name) {
            return new SourceConfig(sheetIndex, name, headerRowIndex, strictBlankRows);
        }

        public SourceConfig headerRowIndex(int index) {
            return new SourceConfig(sheetIndex, sheetName, index, strictBlankRows);
        }

        public SourceConfig strictBlankRows(boolean value) {
            return new SourceConfig(sheetIndex, sheetName, headerRowIndex, value);
        }
    }

    /**
     * Write options for xlsx sinks.
     */
    public record SinkConfig(String sheetName) {
        public SinkConfig {
            if (sheetName == null || sheetName.isBlank()) {
                throw new IllegalArgumentException("sheetName must not be blank");
            }
        }

        public static SinkConfig defaults() {
            return new SinkConfig("Sheet1");
        }
    }
}
