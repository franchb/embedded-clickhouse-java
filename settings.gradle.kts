pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "embedded-clickhouse-java"

include("embedded-clickhouse")
include("embedded-clickhouse-junit4")
include("embedded-clickhouse-junit5")
include("embedded-clickhouse-spring-boot")
include("embedded-clickhouse-bom")
