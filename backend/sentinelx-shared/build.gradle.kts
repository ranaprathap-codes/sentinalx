plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.flywaydb.flyway")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.data:spring-data-commons")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("org.postgresql:postgresql")
    implementation("com.github.ben-manes.caffeine:caffeine")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("org.springframework.kafka:spring-kafka")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.modelmapper:modelmapper:3.2.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:redis")
    testImplementation("org.testcontainers:kafka")
}
flyway {
    url = providers.gradleProperty("flyway.url")
        .orElse("jdbc:postgresql://localhost:5432/sentinelx")
        .get()

    user = providers.gradleProperty("flyway.user")
        .orElse("sentinelx")
        .get()

    password = providers.gradleProperty("flyway.password")
        .orElse("sentinelx_dev")
        .get()

    locations = arrayOf(
        "filesystem:../../infrastructure/db/init"
    )

    baselineOnMigrate = true
}