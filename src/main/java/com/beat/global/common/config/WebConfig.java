package com.beat.global.common.config;

import com.beat.global.auth.resolver.CurrentMemberArgumentResolver;
import com.beat.global.common.converter.StringToEnumCustomConverterFactory;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final CurrentMemberArgumentResolver currentMemberArgumentResolver;

	@Value("${cors.allowed-origins}")
	private String[] allowedOrigins;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentMemberArgumentResolver);
	}

	@Override
	public void addFormatters(final FormatterRegistry registry) {
		final StringToEnumCustomConverterFactory converterFactory = new StringToEnumCustomConverterFactory();
		registry.addConverterFactory(converterFactory);
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
			.allowedOrigins(allowedOrigins)
			.allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
			.allowedHeaders("*")
			.allowCredentials(true);
	}
}
