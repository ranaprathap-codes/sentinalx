plugins {
    id("org.springframework.boot") version "3.3.2" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    id("org.flywaydb.flyway") version "10.21.0" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.spring") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.jpa") version "1.9.24" apply false
    id("com.diffplug.spotless") version "6.25.0" apply false
}

allprojects {
    group = "com.sentinelx"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/snapshot") }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",
                "-Xopt-in=kotlin.RequiresOptIn"
            )
            jvmTarget.set(
                org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
            )
        }
    }

    dependencies {
        add("testImplementation", platform("org.junit:junit-bom:5.11.0"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testImplementation", "org.mockito:mockito-junit-jupiter")
        add("testImplementation", "org.mockito:mockito-kotlin")
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        add("testImplementation", "org.testcontainers:junit-jupiter")
        add("testImplementation", "org.testcontainers:postgresql")
        add("testImplementation", "org.testcontainers:redis")
        add("testImplementation", "org.testcontainers:kafka")
        add("testImplementation", "org.testcontainers:testcontainers-bom:1.20.1")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        systemProperty("spring.profiles.active", "test")
    }
}