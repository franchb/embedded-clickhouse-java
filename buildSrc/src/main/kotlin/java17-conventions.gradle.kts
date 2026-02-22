plugins {
    id("java-library-conventions")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
