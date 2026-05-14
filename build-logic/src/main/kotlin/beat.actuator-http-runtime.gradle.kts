import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("runtimeOnly", libs.findLibrary("spring-boot-starter-web").get())
}
