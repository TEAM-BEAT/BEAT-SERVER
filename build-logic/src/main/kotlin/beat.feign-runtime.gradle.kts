import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("implementation", platform(libs.findLibrary("spring-cloud-dependencies").get()))
    add("implementation", libs.findLibrary("spring-cloud-starter-openfeign").get())
}
