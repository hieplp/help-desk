package dev.hieplp.order.calculator.model;

import java.util.Objects;

/**
 * One column in the output layout: copied from the input or computed.
 */
public sealed interface OutputColumn {

    static PassThrough passThrough(String inputName) {
        return new PassThrough(inputName, inputName);
    }

    static PassThrough passThrough(String inputName, String outputName) {
        return new PassThrough(inputName, outputName);
    }

    static Computed computed(ComputedType type, String outputName) {
        return new Computed(type, outputName);
    }

    static Custom computed(ComputedColumn function, String outputName) {
        return new Custom(function, outputName);
    }

    /**
     * Header name in the output.
     */
    String outputName();

    /**
     * Column copied verbatim from the input, optionally renamed.
     */
    record PassThrough(String inputName, String outputName) implements OutputColumn {
        public PassThrough {
            Objects.requireNonNull(inputName, "inputName");
            Objects.requireNonNull(outputName, "outputName");
        }
    }

    /**
     * Column derived by a built-in {@link ComputedType}.
     */
    record Computed(ComputedType type, String outputName) implements OutputColumn {
        public Computed {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(outputName, "outputName");
        }
    }

    /**
     * Column derived by a caller-provided {@link ComputedColumn}.
     */
    record Custom(ComputedColumn function, String outputName) implements OutputColumn {
        public Custom {
            Objects.requireNonNull(function, "function");
            Objects.requireNonNull(outputName, "outputName");
        }
    }
}
