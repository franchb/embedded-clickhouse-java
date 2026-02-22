plugins {
    id("java8-conventions")
}

description = "Spring Boot auto-configuration for embedded ClickHouse"

dependencies {
    api(project(":embedded-clickhouse"))
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.javax.annotation.api)

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
    testImplementation(libs.slf4j.simple)
}
