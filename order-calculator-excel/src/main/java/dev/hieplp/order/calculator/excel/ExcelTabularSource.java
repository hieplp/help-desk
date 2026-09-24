package dev.hieplp.order.calculator.excel;

import dev.hieplp.order.calculator.error.OrderProcessingException;
import dev.hieplp.order.calculator.spi.TabularRow;
import dev.hieplp.order.calculator.spi.TabularSource;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * {@link TabularSource} backed by Apache POI (XSSF, {@code .xlsx} only).
 * Data ends at the first fully blank row. Numeric role columns read via
 * {@code Cell#getNumericCellValue()} → {@code BigDecimal.valueOf(...)};
 * pass-through cells via {@code DataFormatter}.
 */
final class ExcelTabularSource implements TabularSource {

    private final Path path;
    private final InputStream in;
    private final Excel.SourceConfig config;
    private final DataFormatter formatter = new DataFormatter();

    private Workbook workbook;
    private Sheet sheet;
    private Map<String, Integer> columns;
    private List<String> headerNames;

    ExcelTabularSource(Path path, Excel.SourceConfig config) {
        this.path = Objects.requireNonNull(path, "path");
        this.config = Objects.requireNonNull(config, "config");
        this.in = null;
    }

    ExcelTabularSource(InputStream in, Excel.SourceConfig config) {
        this.in = Objects.requireNonNull(in, "in");
        this.config = Objects.requireNonNull(config, "config");
        this.path = null;
    }

    @SuppressWarnings("resource") // workbook is owned by this source; closed in close()
    private Sheet sheet() {
        if (sheet == null) {
            try {
                workbook = path != null
                        ? WorkbookFactory.create(path.toFile())
                        : WorkbookFactory.create(in);
            } catch (IOException e) {
                throw new OrderProcessingException("failed to open workbook", e);
            }

            sheet = config.sheetName() != null
                    ? workbook.getSheet(config.sheetName())
                    : workbook.getSheetAt(config.sheetIndex());
            if (sheet == null) {
                throw new OrderProcessingException("no sheet named '" + config.sheetName() + "'");
            }
        }
        return sheet;
    }

    private void readHeader() {
        if (columns != null) {
            return;
        }
        Row header = sheet().getRow(config.headerRowIndex());
        if (header == null) {
            throw new OrderProcessingException(
                    "missing header row at index " + config.headerRowIndex());
        }

        columns = new HashMap<>();
        headerNames = new ArrayList<>();
        for (Cell cell : header) {
            String name = formatter.formatCellValue(cell).trim();
            if (columns.putIfAbsent(name, cell.getColumnIndex()) == null) {
                headerNames.add(name);
            }
        }
    }

    @Override
    public List<String> header() {
        readHeader();
        return headerNames;
    }

    @Override
    public Stream<TabularRow> rows() {
        Sheet s = sheet();
        readHeader();

        Iterator<TabularRow> it = new Iterator<>() {
            private int r = config.headerRowIndex() + 1;

            @Override
            public boolean hasNext() {
                return r <= s.getLastRowNum() && !isBlank(s.getRow(r));
            }

            @Override
            public TabularRow next() {
                return new ExcelTabularRow(s.getRow(r++), columns, formatter);
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, 0), false);
    }

    private boolean isBlank(Row row) {
        if (row == null) {
            return true;
        }
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK
                    && !formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() {
        try {
            if (workbook != null) {
                workbook.close();
            } else if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            throw new OrderProcessingException("failed to close workbook", e);
        }
    }

    /**
     * Missing column, missing cell, or a text cell in a numeric column throws —
     * the engine converts failures into {@code RowError}s per the
     * {@code ErrorStrategy}. Formula cells yield their last cached value.
     */
    private record ExcelTabularRow(Row row, Map<String, Integer> columns, DataFormatter formatter)
            implements TabularRow {

        private Cell cell(String column) {
            Integer idx = columns.get(column);
            if (idx == null) {
                throw new IllegalArgumentException("unknown column '" + column + "'");
            }
            return row.getCell(idx);
        }

        @Override
        public String text(String column) {
            return formatter.formatCellValue(cell(column));
        }

        @Override
        public BigDecimal number(String column) {
            Cell c = cell(column);
            if (c == null) {
                throw new IllegalStateException(
                        "empty cell in column '" + column + "' at row " + (row.getRowNum() + 1));
            }
            return BigDecimal.valueOf(c.getNumericCellValue());
        }

        @Override
        public long lineNumber() {
            return row.getRowNum() + 1L;
        }
    }
}
