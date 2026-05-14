import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("implementation", libs.findLibrary("spring-boot-starter-web").get())
    add("implementation", libs.findLibrary("spring-boot-starter-validation").get())
}
