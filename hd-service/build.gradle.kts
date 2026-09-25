plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.2.1"
}

group = "dev.hieplp.helpdesk"
version = "0.0.1-SNAPSHOT"
description = "hd-service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("org.xerial:sqlite-jdbc")
    runtimeOnly("org.hibernate.orm:hibernate-community-dialects")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    java {
        // google-java-format is not configurable; two local rules ride on top:
        // 1. a wrapped parameter list puts ") {" on its own line at declaration indent
        // 2. javadoc @tag continuations align under the tag argument
        googleJavaFormat("1.28.0")
        addStep(
            com.diffplug.spotless.FormatterStep.create(
                "localStyle",
                "v1",
                com.diffplug.spotless.SerializedFunction<String, com.diffplug.spotless.FormatterFunc> {
                    com.diffplug.spotless.FormatterFunc { raw ->
                        val lines = raw.split("\n")

                        // Pass 1: realign javadoc continuations that follow a @tag line.
                        val tagRe = Regex("^(\\s*\\* )(@\\S+ )\\S")
                        val contRe = Regex("^\\s*\\*\\s{2,}(\\S.*)$")
                        var star = ""
                        var tagCol = -1
                        val pass1 = ArrayList<String>(lines.size)
                        for (line in lines) {
                            val tag = tagRe.find(line)
                            if (tag != null) {
                                star = tag.groupValues[1]
                                tagCol = star.length + tag.groupValues[2].length
                                pass1 += line
                                continue
                            }
                            val cont = contRe.find(line)
                            if (cont != null && tagCol > 0) {
                                pass1 += star + " ".repeat(tagCol - star.length) + cont.groupValues[1]
                                continue
                            }
                            if (!line.trimStart().startsWith("*")) tagCol = -1
                            pass1 += line
                        }

                        // Pass 2: ") {" ending a wrapped parameter list moves to its own line.
                        val closeRe = Regex("^(\\s*\\S.*)\\) \\{\\s*$")
                        val keywords =
                            setOf(
                                "if", "for", "while", "catch", "switch", "synchronized",
                                "try", "do", "else", "return", "new", "throw", "assert", "case")
                        val out = ArrayList<String>(pass1.size + 8)
                        for (i in pass1.indices) {
                            val line = pass1[i]
                            val trimmed = line.trimStart()
                            val m = closeRe.find(line)
                            if (m == null || trimmed.startsWith("*") || trimmed.startsWith("//")) {
                                out += line
                                continue
                            }
                            val paramText = m.groupValues[1]
                            if (paramText.isBlank()) {
                                out += line
                                continue
                            }
                            // Find the "(" matching ")" by scanning backwards. Parens inside
                            // annotation string args would miscount; none exist today.
                            var depth = 1
                            var openerLine = -1
                            var openerCol = -1
                            var li = i
                            var c = paramText.length - 1
                            scan@ while (li >= 0 && i - li < 80) {
                                val l = pass1[li]
                                if (li != i) c = l.length - 1
                                while (c >= 0) {
                                    when (l[c]) {
                                        ')' -> depth++
                                        '(' ->
                                            if (--depth == 0) {
                                                openerLine = li
                                                openerCol = c
                                                break@scan
                                            }
                                    }
                                    c--
                                }
                                li--
                            }
                            var moved = false
                            if (openerLine in 0 until i) {
                                val ol = pass1[openerLine]
                                var e = openerCol - 1
                                while (e >= 0 && ol[e] == ' ') e--
                                var s = e
                                while (s >= 0 && (ol[s].isJavaIdentifierPart() || ol[s] == '.')) s--
                                val word = ol.substring(s + 1, e + 1).substringAfterLast('.')
                                if (word.isNotEmpty() && word !in keywords) {
                                    out += paramText.trimEnd()
                                    out += ol.takeWhile { it == ' ' } + ") {"
                                    moved = true
                                }
                            }
                            if (!moved) out += line
                        }
                        out.joinToString("\n")
                    }}))
    }
}
