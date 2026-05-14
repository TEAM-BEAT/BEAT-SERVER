import io.sentry.android.gradle.extensions.SentryPluginExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("io.sentry.jvm.gradle")
}

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val sentrySdkVersion = libsCatalog.findVersion("sentry").get().requiredVersion

configurations.configureEach {
    resolutionStrategy.force("io.sentry:sentry:$sentrySdkVersion")
    resolutionStrategy.eachDependency {
        if (requested.group == "io.sentry") {
            useVersion(sentrySdkVersion)
            because("Keep Sentry SDK artifacts aligned with the Boot 4 observability contract")
        }
    }
}

extensions.configure<SentryPluginExtension>("sentry") {
    includeSourceContext.set(true)
    autoUploadSourceContext.set(
        providers.environmentVariable("SENTRY_AUTH_TOKEN").map { it.isNotBlank() }.orElse(false),
    )
    org.set("beat-jo")
    projectName.set("java-spring-boot")
    authToken.set(providers.environmentVariable("SENTRY_AUTH_TOKEN").orElse(""))
    autoInstallation {
        enabled.set(false)
        sentryVersion.set(sentrySdkVersion)
    }
    telemetry.set(false)
}
