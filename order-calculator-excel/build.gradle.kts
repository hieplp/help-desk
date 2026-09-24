dependencies {
    api(project(":order-calculator-core"))
    api("org.apache.poi:poi-ooxml:5.5.1")
    testImplementation(project(":order-calculator-csv"))
}
