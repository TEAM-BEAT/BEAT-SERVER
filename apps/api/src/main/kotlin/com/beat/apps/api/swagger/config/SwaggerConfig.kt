package com.beat.apps.api.swagger.config

import com.beat.apps.api.swagger.annotation.DisableSwaggerSecurity
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

@Profile("!prod")
@Configuration(proxyBeanMethods = false)
class SwaggerConfig(@param:Value("\${app.server.url}") private val serverUrl: String) {
    @Bean
    fun openAPI(): OpenAPI {
        val schemeName = "JWT"
        val scheme =
            SecurityScheme()
                .name(schemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT access token을 Bearer 방식으로 입력합니다. 예: Bearer {token}")
        return OpenAPI()
            .addServersItem(Server().url(serverUrl).description("BEAT 사용자 API 서버"))
            .components(Components().addSecuritySchemes(schemeName, scheme))
            .info(apiInfo())
            .addSecurityItem(SecurityRequirement().addList(schemeName))
    }

    @Bean
    fun generalApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("general")
            .pathsToMatch("/**")
            .addOpenApiCustomizer(customizeImagePresignedUploadSchema())
            .addOperationCustomizer(customize())
            .build()

    private fun customizeImagePresignedUploadSchema(): OpenApiCustomizer =
        OpenApiCustomizer { openAPI ->
            val schema = openAPI.components?.schemas?.get("ImagePresignedUpload")
            if (schema != null) {
                if (schema.description.isNullOrBlank()) {
                    schema.description = "S3 이미지 업로드용 presigned URL과 저장될 object key입니다."
                }
                schema.properties?.get("uploadUrl")?.let { uploadUrl ->
                    if (uploadUrl.description.isNullOrBlank()) {
                        uploadUrl.description = "이미지 파일을 업로드할 S3 PUT presigned URL입니다."
                    }
                }
                schema.properties?.get("imageKey")?.let { imageKey ->
                    if (imageKey.description.isNullOrBlank()) {
                        imageKey.description = "업로드된 이미지를 식별하는 S3 object key입니다."
                    }
                }
            }
        }

    @Bean
    fun customize(): OperationCustomizer = OperationCustomizer { operation, handlerMethod ->
        val securityDisabled =
            handlerMethod.getMethodAnnotation(DisableSwaggerSecurity::class.java) != null
        if (securityDisabled) {
            operation.security = emptyList()
        }
        val responses = operation.responses ?: ApiResponses().also { operation.responses = it }
        if (!securityDisabled) {
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
            .title("BEAT 사용자 API")
            .description("공연 등록자와 관객이 공연을 등록·조회하고 티켓을 예매·관리하는 사용자용 API입니다.")
            .version("2.0.0")
}
