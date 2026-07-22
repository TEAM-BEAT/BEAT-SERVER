package com.beat.apis.swagger.config

import com.beat.apis.swagger.annotation.DisableSwaggerSecurity
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("!prod")
@Configuration(proxyBeanMethods = false)
class SwaggerConfig(
    @param:Value("\${app.server.url}")
    private val serverUrl: String,
) {
    @Bean
    fun openAPI(): OpenAPI {
        val schemeName = "JWT"
        val scheme = SecurityScheme()
            .name(schemeName)
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
        return OpenAPI()
            .addServersItem(Server().url(serverUrl))
            .components(Components().addSecuritySchemes(schemeName, scheme))
            .info(apiInfo())
            .addSecurityItem(SecurityRequirement().addList(schemeName))
    }

    @Bean
    fun generalApi(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("general")
        .pathsToMatch("/**")
        .addOperationCustomizer(customize())
        .build()

    @Bean
    fun customize(): OperationCustomizer = OperationCustomizer { operation, handlerMethod ->
        if (handlerMethod.getMethodAnnotation(DisableSwaggerSecurity::class.java) != null) {
            operation.security = emptyList()
        }
        operation
    }

    private fun apiInfo(): Info = Info()
        .title("BEAT Project API")
        .description("간편하게 소규모 공연을 등록하고 관리할 수 있는 티켓 예매 플랫폼")
        .version("1.2.7")
}
