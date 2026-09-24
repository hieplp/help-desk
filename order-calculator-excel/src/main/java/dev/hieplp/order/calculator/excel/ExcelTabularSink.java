package dev.hieplp.order.calculator.excel;

import dev.hieplp.order.calculator.error.OrderProcessingException;
import dev.hieplp.order.calculator.model.CellValue;
import dev.hieplp.order.calculator.spi.TabularSink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * {@link TabularSink} backed by Apache POI SXSSF — constant-memory streaming
 * writes for large sheets. {@link CellValue.Number} cells are written as real
 * numeric cells, {@link CellValue.Text} as string cells.
 */
final class ExcelTabularSink implements TabularSink {

    private final Path path;
    private final OutputStream out;
    private final Excel.SinkConfig config;

    private SXSSFWorkbook workbook;
    private Sheet sheet;
    private int rowIndex;

    ExcelTabularSink(Path path, Excel.SinkConfig config) {
        this.path = Objects.requireNonNull(path, "path");
        this.config = Objects.requireNonNull(config, "config");
        this.out = null;
    }

    ExcelTabularSink(OutputStream out, Excel.SinkConfig config) {
        this.out = Objects.requireNonNull(out, "out");
        this.config = Objects.requireNonNull(config, "config");
        this.path = null;
    }

    private Sheet sheet() {
        if (sheet == null) {
            workbook = new SXSSFWorkbook();
            sheet = workbook.createSheet(config.sheetName());
        }
        return sheet;
    }

    @Override
    public void writeHeader(List<String> columns) {
        Row row = sheet().createRow(rowIndex++);
        for (int i = 0; i < columns.size(); i++) {
            row.createCell(i).setCellValue(columns.get(i));
        }
    }

    @Override
    public void writeRow(List<CellValue> cells) {
        Row row = sheet().createRow(rowIndex++);
        for (int i = 0; i < cells.size(); i++) {
            switch (cells.get(i)) {
                case CellValue.Text t -> row.createCell(i).setCellValue(t.value());
                case CellValue.Number n -> row.createCell(i).setCellValue(n.value().doubleValue());
            }
        }
    }

    @Override
    public void close() {
        if (workbook == null) {
            // Nothing written — still emit a valid empty workbook.
            sheet();
        }

        try (OutputStream os = path != null ? Files.newOutputStream(path) : out) {
            workbook.write(os);
        } catch (IOException e) {
            throw new OrderProcessingException("failed to write workbook", e);
        } finally {
            try {
                workbook.close(); // SXSSFWorkbook.close() also disposes temp files
            } catch (IOException e) {
                throw new OrderProcessingException("failed to close workbook", e);
            }
        }
    }
}
