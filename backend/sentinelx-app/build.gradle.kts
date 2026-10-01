plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":sentinelx-shared"))
    implementation(project(":sentinelx-auth"))
    implementation(project(":sentinelx-payments"))
    implementation(project(":sentinelx-risk"))
    implementation(project(":sentinelx-threatlab"))
    implementation(project(":sentinelx-community"))
    implementation(project(":sentinelx-dryrun"))
    implementation(project(":sentinelx-ops"))
    implementation(project(":sentinelx-ai"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.kafka:spring-kafka")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

springBoot {
    mainClass.set("com.sentinelx.SentinelXApplicationKt")
}