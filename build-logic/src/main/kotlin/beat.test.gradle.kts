plugins {}

tasks.withType<Test>().configureEach {
    // Keep JUnit Platform as the common execution contract for Java tests, Spring Boot tests,
    // and any future Kotlin Kotest/MockK layer that runs through the same Gradle test task.
    useJUnitPlatform()
}
