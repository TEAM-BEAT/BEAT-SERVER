package com.beat.infra.external.cdn;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.beat.global.support.jackson.CdnImageUrlSerializer;

import jakarta.annotation.PostConstruct;

/**
 * Wires the configured CDN base into {@link CdnImageUrlSerializer} once at startup,
 * so {@code @CdnImageUrl}-annotated response fields are rendered with the CDN host.
 */
@Configuration(proxyBeanMethods = false)
public class CdnImageUrlConfig {

	@Value("${cloud.cdn.domain:}")
	private String cdnDomain;

	@PostConstruct
	void initSerializer() {
		CdnImageUrlSerializer.initialize(cdnDomain);
	}
}
