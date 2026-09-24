package dev.hieplp.order.calculator.excel;

import dev.hieplp.order.calculator.error.OrderProcessingException;
import dev.hieplp.order.calculator.spi.TabularRow;
import dev.hieplp.order.calculator.spi.TabularSource;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * {@link TabularSource} backed by Apache POI's event model (XSSFReader +
 * StAX pull-parsing, {@code .xlsx} only). Rows stream one at a time — O(1)
 * memory in row count; only the shared-strings table is held in memory.
 *
 * <p>Data ends at the first fully blank row (or row gap) unless
 * {@link Excel.SourceConfig#strictBlankRows()} is set, in which case a blank
 * row surfaces as a row error and reading continues. Numeric role columns
 * read the raw stored value; pass-through cells render the display text via
 * {@code DataFormatter.formatRawCellContents}. Formula cells yield their last
 * cached value.
 */
final class ExcelTabularSource implements TabularSource {

    private final Path path;
    private final InputStream in;
    private final Excel.SourceConfig config;
    private final DataFormatter formatter = new DataFormatter();

    private OPCPackage pkg;
    private SharedStrings sharedStrings;
    private StylesTable styles;
    private InputStream sheetIn;
    private XMLStreamReader xml;
    private int lastRowNum; // 1-based sheet row of the last parsed <row>

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

    // ---- workbook / sheet wiring ----

    private static int columnIndex(String ref) {
        int col = 0;
        for (int i = 0; i < ref.length(); i++) {
            char ch = ref.charAt(i);
            if (!Character.isLetter(ch)) {
                break;
            }
            col = col * 26 + (Character.toUpperCase(ch) - 'A' + 1);
        }
        return col - 1;
    }

    private XMLStreamReader xml() {
        if (xml == null) {
            try {
                pkg = path != null ? OPCPackage.open(path.toFile()) : OPCPackage.open(in);
                XSSFReader reader = new XSSFReader(pkg);
                sharedStrings = reader.getSharedStringsTable();
                styles = reader.getStylesTable();
                sheetIn = findSheet(reader);
                XMLInputFactory factory = XMLInputFactory.newFactory();
                factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
                factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
                xml = factory.createXMLStreamReader(sheetIn);
                skipToSheetData();
            } catch (OrderProcessingException e) {
                throw e;
            } catch (Exception e) {
                throw new OrderProcessingException("failed to open workbook", e);
            }
        }
        return xml;
    }

    private InputStream findSheet(XSSFReader reader) throws Exception {
        XSSFReader.SheetIterator it = reader.getSheetIterator();
        int index = 0;
        while (it.hasNext()) {
            InputStream stream = it.next();
            boolean match = config.sheetName() != null
                    ? config.sheetName().equals(it.getSheetName())
                    : index == config.sheetIndex();
            if (match) {
                return stream;
            }
            stream.close();
            index++;
        }
        throw new OrderProcessingException(
                config.sheetName() != null
                        ? "no sheet named '" + config.sheetName() + "'"
                        : "no sheet at index " + config.sheetIndex());
    }

    // ---- sheet XML parsing (pull, one row at a time) ----

    private void skipToSheetData() throws XMLStreamException {
        while (xml.hasNext()) {
            if (xml.next() == XMLStreamConstants.START_ELEMENT
                    && "sheetData".equals(xml.getLocalName())) {
                return;
            }
        }
    }

    private RawRow nextRawRow() {
        XMLStreamReader x = xml();
        try {
            while (x.hasNext()) {
                int ev = x.next();
                if (ev == XMLStreamConstants.END_ELEMENT && "sheetData".equals(x.getLocalName())) {
                    return null;
                }
                if (ev == XMLStreamConstants.START_ELEMENT && "row".equals(x.getLocalName())) {
                    return parseRow(x);
                }
            }
            return null;
        } catch (XMLStreamException e) {
            throw new OrderProcessingException("failed to parse sheet xml", e);
        }
    }

    private RawRow parseRow(XMLStreamReader x) throws XMLStreamException {
        String rAttr = x.getAttributeValue(null, "r");
        int rowNum = rAttr != null ? Integer.parseInt(rAttr) : lastRowNum + 1;
        lastRowNum = rowNum;
        Map<Integer, RawCell> cells = new TreeMap<>();
        int nextCol = 0;
        while (x.hasNext()) {
            int ev = x.next();
            if (ev == XMLStreamConstants.START_ELEMENT && "c".equals(x.getLocalName())) {
                RawCell cell = parseCell(x, nextCol);
                cells.put(cell.col(), cell);
                nextCol = cell.col() + 1;
            } else if (ev == XMLStreamConstants.END_ELEMENT && "row".equals(x.getLocalName())) {
                break;
            }
        }
        return new RawRow(rowNum, cells);
    }

    private RawCell parseCell(XMLStreamReader x, int nextCol) throws XMLStreamException {
        String ref = x.getAttributeValue(null, "r");
        int col = ref != null ? columnIndex(ref) : nextCol;
        String type = x.getAttributeValue(null, "t");
        String sAttr = x.getAttributeValue(null, "s");
        int style = sAttr != null ? Integer.parseInt(sAttr) : -1;
        String value = null;
        StringBuilder inline = null;
        while (x.hasNext()) {
            int ev = x.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                switch (x.getLocalName()) {
                    case "v" -> value = x.getElementText();
                    case "f" -> x.getElementText();
                    case "is" -> inline = parseInlineString(x);
                    default -> {
                    }
                }
            } else if (ev == XMLStreamConstants.END_ELEMENT && "c".equals(x.getLocalName())) {
                break;
            }
        }
        return new RawCell(col, type, style, value, inline == null ? null : inline.toString());
    }

    private StringBuilder parseInlineString(XMLStreamReader x) throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        while (x.hasNext()) {
            int ev = x.next();
            if (ev == XMLStreamConstants.START_ELEMENT && "t".equals(x.getLocalName())) {
                sb.append(x.getElementText());
            } else if (ev == XMLStreamConstants.END_ELEMENT && "is".equals(x.getLocalName())) {
                break;
            }
        }
        return sb;
    }

    private XSSFCellStyle styleAt(int index) {
        if (styles == null || index < 0) {
            return null;
        }
        try {
            return styles.getStyleAt(index);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String sharedString(String index) {
        if (index == null || sharedStrings == null) {
            return "";
        }
        int i = Integer.parseInt(index);
        if (i < 0 || i >= sharedStrings.getUniqueCount()) {
            return "";
        }
        return sharedStrings.getItemAt(i).getString();
    }

    // ---- cell rendering ----

    /**
     * Display text equivalent to {@code DataFormatter.formatCellValue}: the
     * what-you-see text for pass-through columns and header names.
     */
    private String displayText(RawCell cell) {
        if (cell == null) {
            return "";
        }
        try {
            return switch (cell.type() == null ? "n" : cell.type()) {
                case "s" -> sharedString(cell.value());
                case "inlineStr" -> cell.inline() == null ? "" : cell.inline();
                case "str", "d" -> cell.value() == null ? "" : cell.value();
                case "e" -> cell.value() == null ? "" : cell.value();
                case "b" -> "1".equals(cell.value()) ? "TRUE" : "FALSE";
                default -> {
                    if (cell.value() == null || cell.value().isEmpty()) {
                        yield "";
                    }
                    XSSFCellStyle style = styleAt(cell.style());
                    int formatIndex = style == null ? 0 : style.getDataFormat();
                    String format = style == null || style.getDataFormatString() == null
                            ? "General"
                            : style.getDataFormatString();
                    yield formatter.formatRawCellContents(
                            Double.parseDouble(cell.value()), formatIndex, format, false);
                }
            };
        } catch (RuntimeException e) {
            return "";
        }
    }

    private boolean isBlank(RawRow row) {
        for (RawCell cell : row.cells().values()) {
            if (!displayText(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private void readHeader() {
        if (columns != null) {
            return;
        }
        xml();
        int target = config.headerRowIndex() + 1; // 1-based sheet row
        RawRow header;
        while ((header = nextRawRow()) != null) {
            if (header.rowNum() < target) {
                continue;
            }
            if (header.rowNum() > target) {
                break;
            }
            columns = new HashMap<>();
            headerNames = new ArrayList<>();
            for (RawCell cell : header.cells().values()) {
                String name = displayText(cell).trim();
                if (columns.putIfAbsent(name, cell.col()) != null) {
                    throw new OrderProcessingException("duplicate header name: '" + name + "'");
                }
                headerNames.add(name);
            }
            return;
        }
        throw new OrderProcessingException(
                "missing header row at index " + config.headerRowIndex());
    }

    @Override
    public List<String> header() {
        readHeader();
        return List.copyOf(headerNames);
    }

    // ---- SPI ----

    @Override
    public Stream<TabularRow> rows() {
        readHeader();

        Iterator<TabularRow> it = new Iterator<>() {
            private int expected = config.headerRowIndex() + 2; // next 1-based sheet row
            private TabularRow pending;
            private RawRow stashed;
            private boolean done;

            @Override
            public boolean hasNext() {
                if (pending != null) {
                    return true;
                }
                if (done) {
                    return false;
                }
                RawRow row = stashed != null ? stashed : nextRawRow();
                stashed = null;
                while (row != null) {
                    boolean gap = row.rowNum() > expected;
                    if (gap || isBlank(row)) {
                        if (!config.strictBlankRows()) {
                            done = true;
                            return false;
                        }
                        pending = blankRowError(expected);
                        if (gap) {
                            stashed = row;
                            expected = row.rowNum(); // stashed row still to be emitted
                        } else {
                            expected = row.rowNum() + 1;
                        }
                        return true;
                    }
                    if (row.rowNum() < expected) {
                        row = nextRawRow();
                        continue;
                    }
                    pending = new ExcelTabularRow(row);
                    expected = row.rowNum() + 1;
                    return true;
                }
                done = true;
                return false;
            }

            @Override
            public TabularRow next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                TabularRow row = pending;
                pending = null;
                return row;
            }
        };
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false);
    }

    private TabularRow blankRowError(int rowNum) {
        long line = rowNum - config.headerRowIndex() - 1L;
        return new TabularRow() {
            private IllegalStateException error() {
                return new IllegalStateException("blank row at line " + line);
            }

            @Override
            public String text(String column) {
                throw error();
            }

            @Override
            public BigDecimal number(String column) {
                throw error();
            }

            @Override
            public long lineNumber() {
                return line;
            }
        };
    }

    @Override
    public void close() {
        OrderProcessingException failure = null;
        try {
            if (xml != null) {
                xml.close();
            }
        } catch (XMLStreamException e) {
            failure = new OrderProcessingException("failed to close workbook", e);
        }
        for (AutoCloseable resource : new AutoCloseable[]{sheetIn, pkg, in}) {
            if (resource == null) {
                continue;
            }
            try {
                resource.close();
            } catch (Exception e) {
                if (failure == null) {
                    failure = new OrderProcessingException("failed to close workbook", e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private record RawCell(int col, String type, int style, String value, String inline) {
    }

    private record RawRow(int rowNum, Map<Integer, RawCell> cells) {
    }

    /**
     * Missing column, missing cell, or a non-numeric cell in a numeric column
     * throws — the engine converts failures into {@code RowError}s per the
     * {@code ErrorStrategy}. Formula cells yield their last cached value.
     */
    private final class ExcelTabularRow implements TabularRow {

        private final RawRow row;

        ExcelTabularRow(RawRow row) {
            this.row = row;
        }

        private RawCell cell(String column) {
            Integer idx = columns.get(column);
            if (idx == null) {
                throw new IllegalArgumentException("unknown column '" + column + "'");
            }
            return row.cells().get(idx);
        }

        @Override
        public String text(String column) {
            return displayText(cell(column));
        }

        @Override
        public BigDecimal number(String column) {
            RawCell c = cell(column);
            long line = lineNumber();
            if (c != null && c.type() != null && !"n".equals(c.type())) {
                throw new IllegalStateException(
                        "non-numeric cell in column '" + column + "' at line " + line);
            }
            if (c == null || c.value() == null || c.value().isBlank()) {
                throw new IllegalStateException(
                        "empty cell in column '" + column + "' at line " + line);
            }
            try {
                return new BigDecimal(c.value());
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                        "non-numeric value '" + c.value() + "' in column '" + column
                                + "' at line " + line);
            }
        }

        @Override
        public long lineNumber() {
            return row.rowNum() - config.headerRowIndex() - 1L;
        }
    }
}
