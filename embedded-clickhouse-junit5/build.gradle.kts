plugins {
    id("java-library-conventions")
}

description = "JUnit 5 Extension for embedded ClickHouse"

dependencies {
    api(project(":embedded-clickhouse"))
    api(libs.junit.jupiter.api)

    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
    testImplementation(libs.slf4j.simple)
}
