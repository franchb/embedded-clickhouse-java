// Resolve the kotlin-dsl plugin's Kotlin artifacts from Maven Central as well,
// not only the Gradle Plugin Portal (a cold portal lookup broke CodeQL on main).
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
