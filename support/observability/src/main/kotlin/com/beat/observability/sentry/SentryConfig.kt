package com.beat.observability.sentry

import io.sentry.Sentry
import io.sentry.SentryOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class SentryConfig(
    @param:Value("\${spring.application.name:beat-server}")
    private val moduleName: String,
    @param:Value("\${sentry.release:}")
    private val configuredRelease: String = "",
) {

    @Bean
    fun beatSentryEventProcessor(): BeatSentryEventProcessor = BeatSentryEventProcessor(moduleName)

    @Bean
    fun beatSentryMetrics(): BeatSentryMetrics = BeatSentryMetrics()

    @Bean
    fun beatSentryOptionsConfiguration(
        beatSentryEventProcessor: BeatSentryEventProcessor,
    ): Sentry.OptionsConfiguration<SentryOptions> = Sentry.OptionsConfiguration { options ->
        if (options.dsn.isNullOrBlank()) {
            options.isEnabled = false
            return@OptionsConfiguration
        }

        if (options.serverName.isNullOrBlank()) {
            options.serverName = moduleName
        }
        if (options.release.isNullOrBlank()) {
            options.release = configuredRelease.takeIf { it.isNotBlank() }
        }
        options.addEventProcessor(beatSentryEventProcessor)
    }
}
