package com.beat.infra.external.cdn;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.beat.contracts.cdn.ImageCachePort;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ImageCacheAdapter implements ImageCachePort {

    private static final List<Integer> TARGET_WIDTHS = List.of(480, 960);
    private static final List<String> TARGET_FORMATS = List.of("image/avif", "image/webp", "image/jpeg");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final Executor VARIANT_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final RestClient restClient;
    private final String cdnBase;

    public ImageCacheAdapter(RestClient.Builder restClientBuilder,
                             @Value("${cloud.cdn.domain:}") String cdnDomain) {
        this.restClient = restClientBuilder
                .requestFactory(buildRequestFactory())
                .build();
        this.cdnBase = normalize(cdnDomain);
    }

    private static JdkClientHttpRequestFactory buildRequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    private static boolean isAbsoluteUrl(String value) {
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static String normalize(String domain) {
        if (domain == null || domain.isBlank()) {
            return "";
        }
        return domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
    }

    @Async("beatAsyncExecutor")
    @Override
    public void preWarm(String imageKey) {
        if (imageKey == null || imageKey.isBlank() || cdnBase.isEmpty()) {
            return;
        }
        String normalizedKey = imageKey.startsWith("/") ? imageKey.substring(1) : imageKey;
        if (isAbsoluteUrl(normalizedKey)) {
            log.debug("Skipping pre-warm for full URL value: {}", imageKey);
            return;
        }
        String baseUrl = cdnBase + "/" + normalizedKey;
        CompletableFuture<?>[] variantTasks = TARGET_WIDTHS.stream()
                .flatMap(width -> TARGET_FORMATS.stream()
                        .map(accept -> CompletableFuture.runAsync(
                                () -> warmSingleVariant(baseUrl, width, accept), VARIANT_EXECUTOR)))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(variantTasks).join();
        log.info("CDN pre-warm completed for {} ({} variants)", baseUrl, variantTasks.length);
    }

    private void warmSingleVariant(String baseUrl, int width, String accept) {
        String targetUrl = baseUrl + "?w=" + width;
        try {
            restClient.get()
                    .uri(targetUrl)
                    .header(HttpHeaders.ACCEPT, accept)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("CDN pre-warm failed: {} [{}] — {}", targetUrl, accept, e.getMessage());
        }
    }
}
