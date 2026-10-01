dependencies {
    implementation(project(":sentinelx-shared"))
    implementation(project(":sentinelx-payments"))
    implementation(project(":sentinelx-risk"))
    implementation(project(":sentinelx-community"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}