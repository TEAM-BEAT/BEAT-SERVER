package com.beat.infrastructure.external.cdn

import com.beat.application.admin.promotion.command.PromotionImageCache
import java.net.http.HttpClient
import java.time.Duration
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
internal class ImageCacheAdapter(
    restClientBuilder: RestClient.Builder,
    @Value("\${cloud.cdn.domain:}") cdnDomain: String,
) : PromotionImageCache {
    private val restClient: RestClient =
        restClientBuilder.requestFactory(buildRequestFactory()).build()
    private val cdnBase: String = normalize(cdnDomain)

    override fun preWarm(imageKey: String) {
        if (imageKey.isBlank() || cdnBase.isEmpty()) {
            return
        }
        val normalizedKey = imageKey.removePrefix("/")
        if (isAbsoluteUrl(normalizedKey)) {
            log.debug("Skipping pre-warm for full URL value: {}", imageKey)
            return
        }
        val baseUrl = "$cdnBase/$normalizedKey"
        val variantTasks =
            TARGET_WIDTHS.flatMap { width ->
                    TARGET_FORMATS.map { accept ->
                        CompletableFuture.runAsync(
                            { warmSingleVariant(baseUrl, width, accept) },
                            VARIANT_EXECUTOR,
                        )
                    }
                }
                .toTypedArray()
        CompletableFuture.allOf(*variantTasks).join()
        log.info("CDN pre-warm completed for {} ({} variants)", baseUrl, variantTasks.size)
    }

    private fun warmSingleVariant(baseUrl: String, width: Int, accept: String) {
        val targetUrl = "$baseUrl?w=$width"
        try {
            restClient
                .get()
                .uri(targetUrl)
                .header(HttpHeaders.ACCEPT, accept)
                .retrieve()
                .toBodilessEntity()
        } catch (exception: RestClientException) {
            log.warn("CDN pre-warm failed: {} [{}] — {}", targetUrl, accept, exception.message)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(ImageCacheAdapter::class.java)
        val TARGET_WIDTHS = listOf(240, 480, 960)
        val TARGET_FORMATS = listOf("image/avif", "image/webp", "image/jpeg")
        val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(2)
        val READ_TIMEOUT: Duration = Duration.ofSeconds(5)
        val VARIANT_EXECUTOR: Executor = Executors.newVirtualThreadPerTaskExecutor()

        fun buildRequestFactory(): JdkClientHttpRequestFactory {
            val httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build()
            return JdkClientHttpRequestFactory(httpClient).apply { setReadTimeout(READ_TIMEOUT) }
        }

        fun isAbsoluteUrl(value: String): Boolean {
            val lower = value.lowercase(Locale.ROOT)
            return lower.startsWith("http://") || lower.startsWith("https://")
        }

        fun normalize(domain: String?): String {
            if (domain.isNullOrBlank()) {
                return ""
            }
            return domain.replace(Regex("/+$"), "")
        }
    }
}
