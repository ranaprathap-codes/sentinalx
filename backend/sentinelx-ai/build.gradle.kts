plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

tasks.named("bootJar") {
    (this as org.springframework.boot.gradle.tasks.bundling.BootJar).enabled = false
}

tasks.named("jar") {
    (this as org.gradle.jvm.tasks.Jar).enabled = true
}

dependencies {
    implementation(project(":sentinelx-shared"))
    implementation(project(":sentinelx-payments"))
    implementation(project(":sentinelx-risk"))
    implementation(project(":sentinelx-community"))
    implementation(project(":sentinelx-ops"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}