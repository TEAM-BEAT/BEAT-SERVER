package com.beat.admin.swagger.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Profile("!prod")
@Configuration(proxyBeanMethods = false)
public class AdminSwaggerConfig {

	@Value("${app.server.url:}")
	private String serverUrl;

	@Bean
	public OpenAPI openAPI() {
		String jwt = "JWT";
		SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwt);
		Components components = new Components().addSecuritySchemes(jwt, new SecurityScheme()
			.name(jwt)
			.type(SecurityScheme.Type.HTTP)
			.scheme("bearer")
			.bearerFormat("JWT")
		);

		OpenAPI openAPI = new OpenAPI()
			.components(components)
			.info(apiInfo())
			.addSecurityItem(securityRequirement);

		if (StringUtils.hasText(serverUrl)) {
			openAPI.addServersItem(new Server().url(serverUrl));
		}

		return openAPI;
	}

	@Bean
	public GroupedOpenApi adminApi() {
		return GroupedOpenApi.builder()
			.group("admin")
			.pathsToMatch("/api/admin/**")
			.build();
	}

	private Info apiInfo() {
		return new Info()
			.title("BEAT Admin API")
			.description("BEAT 관리자/백오피스 운영 API")
			.version("1.2.7");
	}
}
