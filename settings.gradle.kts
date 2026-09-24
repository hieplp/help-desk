rootProject.name = "order-calculator"

plugins {
    // Auto-provisions a JDK 21 for the toolchain when none is installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

include("order-calculator-core")
include("order-calculator-csv")
include("order-calculator-excel")
include("order-calculator-app")
