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
    implementation(project(":sentinelx-auth"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}