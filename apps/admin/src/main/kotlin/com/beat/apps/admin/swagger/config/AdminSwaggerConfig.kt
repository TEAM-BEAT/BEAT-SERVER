package com.beat.apps.admin.swagger.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.util.StringUtils

@Profile("!prod")
@Configuration(proxyBeanMethods = false)
class AdminSwaggerConfig(@param:Value("\${app.server.url:}") private val serverUrl: String) {
    @Bean
    fun openAPI(): OpenAPI {
        val jwt = "JWT"
        val securityRequirement = SecurityRequirement().addList(jwt)
        val components =
            Components()
                .addSecuritySchemes(
                    jwt,
                    SecurityScheme()
                        .name(jwt)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT access token을 Bearer 방식으로 입력합니다. 예: Bearer {token}"),
                )

        val openAPI =
            OpenAPI().components(components).info(apiInfo()).addSecurityItem(securityRequirement)

        if (StringUtils.hasText(serverUrl)) {
            openAPI.addServersItem(Server().url(serverUrl).description("BEAT 관리자 API 서버"))
        }

        return openAPI
    }

    @Bean
    fun adminApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("admin")
            .pathsToMatch("/api/admin/**")
            .addOpenApiCustomizer(customizePromotionHandleRequestSchema())
            .addOperationCustomizer(customize())
            .build()

    private fun customizePromotionHandleRequestSchema(): OpenApiCustomizer =
        OpenApiCustomizer { openAPI ->
            val typeSchema =
                openAPI.components?.schemas?.get("PromotionHandleRequest")?.properties?.get("type")
            if (typeSchema != null) {
                typeSchema.description =
                    "요청 항목 유형입니다. modify는 기존 프로모션 수정, generate는 신규 프로모션 생성입니다."
                typeSchema.example = "generate"
            }
        }

    @Bean
    fun customize(): OperationCustomizer = OperationCustomizer { operation, _ ->
        val responses = operation.responses ?: ApiResponses().also { operation.responses = it }
        if (!responses.containsKey("401")) {
            responses.addApiResponse(
                "401",
                ApiResponse().description("JWT 인증 정보가 없거나 유효하지 않거나 만료되었습니다. 응답 본문은 없습니다."),
            )
        }
        if (!responses.containsKey("403")) {
            responses.addApiResponse(
                "403",
                ApiResponse().description("인증되었지만 요청한 리소스에 접근할 권한이 없습니다. 응답 본문은 없습니다."),
            )
        }
        if (!responses.containsKey("400")) {
            responses.addApiResponse(
                "400",
                errorResponse("요청 파라미터 또는 본문 형식이 잘못되었거나 검증에 실패했습니다."),
            )
        }
        if (!responses.containsKey("500")) {
            responses.addApiResponse(
                "500",
                errorResponse("처리되지 않은 서버 오류가 발생했습니다."),
            )
        }
        operation
    }

    private fun errorResponse(description: String): ApiResponse =
        ApiResponse()
            .description(description)
            .content(
                Content()
                    .addMediaType(
                        "application/json",
                        MediaType()
                            .schema(Schema<Any>().`$ref`("#/components/schemas/ErrorResponse")),
                    )
            )

    private fun apiInfo(): Info =
        Info()
            .title("BEAT 관리자 API")
            .description("관리자가 공연·회원·홍보 콘텐츠를 운영하는 관리자용 API입니다.")
            .version("2.0.0")
}
