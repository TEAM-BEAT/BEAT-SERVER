plugins {}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
