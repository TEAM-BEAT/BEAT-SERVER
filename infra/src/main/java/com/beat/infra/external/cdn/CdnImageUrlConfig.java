package com.beat.infra.external.cdn;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.beat.global.support.jackson.CdnImageUrlSerializer;

@Configuration(proxyBeanMethods = false)
public class CdnImageUrlConfig {

	@Value("${cloud.cdn.domain:}")
	private String cdnDomain;

	@PostConstruct
	void initSerializer() {
		CdnImageUrlSerializer.initialize(cdnDomain);
	}
}
