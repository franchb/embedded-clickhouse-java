plugins {
    id("java8-conventions")
}

description = "JUnit 4 Rule for embedded ClickHouse"

dependencies {
    api(project(":embedded-clickhouse"))
    api(libs.junit4)

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.slf4j.simple)
}
