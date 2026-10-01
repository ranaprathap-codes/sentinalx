dependencies {
    implementation(project(":sentinelx-shared"))
    implementation(project(":sentinelx-payments"))
    implementation(project(":sentinelx-risk"))
    implementation(project(":sentinelx-community"))
    implementation(project(":sentinelx-ops"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.ai:spring-ai-openai:0.8.1")
    implementation("org.springframework.ai:spring-ai-ollama:0.8.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}