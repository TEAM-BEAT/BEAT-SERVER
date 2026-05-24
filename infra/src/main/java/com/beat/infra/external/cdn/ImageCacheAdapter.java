package com.beat.infra.external.cdn;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.beat.contracts.cdn.ImageCachePort;

import lombok.extern.slf4j.Slf4j;

/**
 * Infrastructure adapter that pre-warms the CDN by issuing GETs for the
 * common width variants of a freshly registered image key.
 */
@Slf4j
@Component
public class ImageCacheAdapter implements ImageCachePort {

    private static final List<Integer> TARGET_WIDTHS = List.of(480, 960);

    private final RestClient restClient;
    private final String cdnBase;

    public ImageCacheAdapter(RestClient.Builder restClientBuilder,
                             @Value("${cloud.cdn.domain:}") String cdnDomain) {
        this.restClient = restClientBuilder.build();
        this.cdnBase = normalize(cdnDomain);
    }

    private static String normalize(String domain) {
        if (domain == null || domain.isBlank()) {
            return "";
        }
        return domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
    }

    @Async
    @Override
    public void preWarm(String imageKey) {
        if (imageKey == null || imageKey.isBlank() || cdnBase.isEmpty()) {
            return;
        }
        String normalizedKey = imageKey.startsWith("/") ? imageKey.substring(1) : imageKey;
        if (normalizedKey.startsWith("http")) {
            log.debug("Skipping pre-warm for full URL value: {}", imageKey);
            return;
        }
        String baseUrl = cdnBase + "/" + normalizedKey;
        for (int width : TARGET_WIDTHS) {
            warmSingleVariant(baseUrl, width);
        }
        log.info("CDN pre-warm completed for {}", baseUrl);
    }

    private void warmSingleVariant(String baseUrl, int width) {
        String targetUrl = baseUrl + "?w=" + width;
        try {
            restClient.get()
                    .uri(targetUrl)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("CDN pre-warm failed: {} — {}", targetUrl, e.getMessage());
        }
    }
}
