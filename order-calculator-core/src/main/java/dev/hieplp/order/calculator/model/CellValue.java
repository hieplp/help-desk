package dev.hieplp.order.calculator.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One output cell. Typed so sinks can do the right thing per format:
 * {@link Text} renders as text (CSV string, Excel string cell), {@link Number}
 * renders as a real number (Excel numeric cell) or plain string (CSV).
 */
public sealed interface CellValue {

    static CellValue text(String value) {
        return new Text(value);
    }

    static CellValue number(BigDecimal value) {
        return new Number(value);
    }

    static CellValue empty() {
        return new Text("");
    }

    record Text(String value) implements CellValue {
        public Text {
            value = Objects.requireNonNullElse(value, "");
        }
    }

    record Number(BigDecimal value) implements CellValue {
        public Number {
            Objects.requireNonNull(value, "value");
        }
    }

}
