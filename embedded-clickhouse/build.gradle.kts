plugins {
    id("java8-conventions")
}

description = "Embedded ClickHouse server for Java — download, start, stop ClickHouse in tests"

dependencies {
    api(libs.slf4j.api)
    implementation(libs.commons.compress)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.core)
    testImplementation(libs.assertj.core)
    testImplementation(libs.slf4j.simple)
}
