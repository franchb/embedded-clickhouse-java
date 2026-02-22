plugins {
    id("java-library-conventions")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}
