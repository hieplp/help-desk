// Test application for exercising the library end-to-end.
// Runnable via `./gradlew :order-calculator-app:run`.
// No module-info: the app runs on the classpath so `run` needs no module-path wiring.
plugins {
    application
}

dependencies {
    implementation(project(":order-calculator-core"))
    implementation(project(":order-calculator-csv"))
    implementation(project(":order-calculator-excel"))
    // poi-ooxml comes in transitively via order-calculator-excel (api dep) —
    // used to generate the sample .xlsx input.
}

application {
    mainClass.set("dev.hieplp.order.app.Main")
}
