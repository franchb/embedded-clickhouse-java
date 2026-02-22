plugins {
    `java-platform`
}

group = "io.github.franchb"
version = rootProject.version

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(project(":embedded-clickhouse"))
        api(project(":embedded-clickhouse-junit4"))
        api(project(":embedded-clickhouse-junit5"))
        api(project(":embedded-clickhouse-spring-boot"))
    }
}
