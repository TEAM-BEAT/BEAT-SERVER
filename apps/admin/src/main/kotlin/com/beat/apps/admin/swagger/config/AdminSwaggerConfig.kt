package com.beat.apps.admin.swagger.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.util.StringUtils

@Profile("!prod")
@Configuration(proxyBeanMethods = false)
class AdminSwaggerConfig(
    @param:Value("\${app.server.url:}")
    private val serverUrl: String,
) {
    @Bean
    fun openAPI(): OpenAPI {
        val jwt = "JWT"
        val securityRequirement = SecurityRequirement().addList(jwt)
        val components = Components().addSecuritySchemes(
            jwt,
            SecurityScheme()
                .name(jwt)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"),
        )

        val openAPI = OpenAPI()
            .components(components)
            .info(apiInfo())
            .addSecurityItem(securityRequirement)

        if (StringUtils.hasText(serverUrl)) {
            openAPI.addServersItem(Server().url(serverUrl))
        }

        return openAPI
    }

    @Bean
    fun adminApi(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("admin")
        .pathsToMatch("/api/admin/**")
        .build()

    private fun apiInfo(): Info = Info()
        .title("BEAT Admin API")
        .description("BEAT 관리자/백오피스 운영 API")
        .version("1.2.7")
}