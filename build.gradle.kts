group = "dev.hieplp.order"
version = "1.0-SNAPSHOT"

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // -classfile: dependency jars carry annotations we can't resolve
        // (e.g. commons-csv's SuppressFBWarnings) — not our code's problem.
        options.compilerArgs.add("-Xlint:all")
        options.compilerArgs.add("-Xlint:-classfile")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        "testImplementation"(platform("org.junit:junit-bom:6.0.0"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                pom {
                    name.set(project.name)
                    description.set(
                        "Streaming tabular order line-item calculator " +
                            "(per-line and order totals before/after tax)."
                    )
                    url.set("https://github.com/hieplp/help-desk")
                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    scm {
                        url.set("https://github.com/hieplp/help-desk")
                        connection.set("scm:git:git://github.com/hieplp/help-desk.git")
                        developerConnection.set("scm:git:git@github.com:hieplp/help-desk.git")
                    }
                    developers {
                        developer {
                            id.set("hieplp")
                            name.set("HiepLP")
                            email.set("hiepphuocly@gmail.com")
                        }
                    }
                }
            }
        }
    }
}
