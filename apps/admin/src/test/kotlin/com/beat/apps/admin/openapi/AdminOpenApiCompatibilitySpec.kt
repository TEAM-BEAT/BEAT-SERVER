package com.beat.apps.admin.openapi

import com.beat.apps.admin.support.BeatAdminAcceptanceTest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@BeatAdminAcceptanceTest
@Tags("openapi")
class AdminOpenApiCompatibilitySpec : FunSpec() {

    @Autowired private lateinit var mockMvc: MockMvc
    private val objectMapper = jacksonObjectMapper()

    init {
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("admin OpenAPI 문서를 생성하고 필수 설명 계약을 검증한다") {
            val result =
                mockMvc
                    .perform(get("/api/admin/v3/api-docs/admin"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.openapi").exists())
                    .andExpect(jsonPath("$.paths").exists())
                    .andReturn()

            objectMapper.readTree(result.response.contentAsString).assertOpenApiContract()

            val outputDirectory =
                Path.of(requireNotNull(System.getProperty("beat.openapi.output.dir")))
            Files.createDirectories(outputDirectory)
            Files.write(outputDirectory.resolve("admin.json"), result.response.contentAsByteArray)
        }
    }
}

private val OPEN_API_HTTP_METHODS =
    setOf("get", "post", "put", "patch", "delete", "options", "head", "trace")

private fun JsonNode.requireNonBlank(
    field: String,
    context: String,
): String {
    val value = get(field)
    val text = if (value?.isTextual == true) value.asText() else null
    check(!text.isNullOrBlank()) { "$context.$field must be a non-blank string" }
    return requireNotNull(text)
}

private fun JsonNode.assertOpenApiContract() {
    val info = get("info") ?: error("OpenAPI info is missing")
    info.requireNonBlank("version", "info") shouldBe "2.0.0"

    val paths = get("paths") ?: error("OpenAPI paths are missing")
    val pathEntries = paths.fields()
    while (pathEntries.hasNext()) {
        val pathEntry = pathEntries.next()
        val path = pathEntry.key
        val pathItem = pathEntry.value
        assertParameterDescriptions(pathItem.get("parameters"), "path $path")

        val operations = pathItem.fields()
        while (operations.hasNext()) {
            val operationEntry = operations.next()
            if (operationEntry.key !in OPEN_API_HTTP_METHODS) continue

            val context = "${operationEntry.key.uppercase()} $path"
            val operation = operationEntry.value
            operation.requireNonBlank("operationId", context)
            operation.requireNonBlank("summary", context)
            operation.requireNonBlank("description", context)
            assertParameterDescriptions(operation.get("parameters"), context)

            val responses = operation.get("responses") ?: error("$context.responses is missing")
            val responseEntries = responses.fields()
            while (responseEntries.hasNext()) {
                val responseEntry = responseEntries.next()
                responseEntry.value.requireNonBlank(
                    "description",
                    "$context response ${responseEntry.key}",
                )
            }
        }
    }

    // 현재 components.schemas의 공개 API DTO와 공통 envelope properties를 계약 대상으로 검증한다.
    // springdoc이 합성한 schema 자체의 설명은 강제하지 않는다.
    val schemas =
        get("components")?.get("schemas") ?: error("OpenAPI component schemas are missing")
    val schemaEntries = schemas.fields()
    while (schemaEntries.hasNext()) {
        val schemaEntry = schemaEntries.next()
        val properties = schemaEntry.value.get("properties") ?: continue
        val propertyEntries = properties.fields()
        while (propertyEntries.hasNext()) {
            val propertyEntry = propertyEntries.next()
            propertyEntry.value.requireNonBlank(
                "description",
                "${schemaEntry.key}.properties.${propertyEntry.key}",
            )
        }
    }

    assertCarouselPresignedMapSchemas(schemas)
    assertAdminCarouselPromotionIdSchemas(paths, schemas)
}

private fun assertAdminCarouselPromotionIdSchemas(
    paths: JsonNode,
    schemas: JsonNode,
) {
    paths
        .get("/api/admin/carousels")
        ?.get("get")
        ?.get("responses")
        ?.get("200")
        ?.get("content")
        ?.get("application/json")
        ?.get("schema")
        ?.get("\$ref")
        ?.asText() shouldBe "#/components/schemas/SuccessResponseCarouselFindAllResponse"

    paths
        .get("/api/admin/carousels")
        ?.get("put")
        ?.get("responses")
        ?.get("200")
        ?.get("content")
        ?.get("application/json")
        ?.get("schema")
        ?.get("\$ref")
        ?.asText() shouldBe "#/components/schemas/SuccessResponseCarouselHandleAllResponse"

    assertIntegerInt64PromotionId(schemas, "CarouselFindResponse")
    assertIntegerInt64PromotionId(schemas, "PromotionResponse")
}

private fun assertIntegerInt64PromotionId(
    schemas: JsonNode,
    schemaName: String,
) {
    val promotionId =
        schemas.get(schemaName)?.get("properties")?.get("promotionId")
            ?: error("$schemaName.promotionId schema is missing")
    promotionId.get("type")?.asText() shouldBe "integer"
    promotionId.get("format")?.asText() shouldBe "int64"
}

private fun assertCarouselPresignedMapSchemas(schemas: JsonNode) {
    val responseSchema =
        schemas.get("CarouselPresignedUrlFindAllResponse")
            ?: error("CarouselPresignedUrlFindAllResponse schema is missing")
    val properties =
        responseSchema.get("properties") ?: error("Carousel response properties are missing")

    val presignedUrls =
        properties.get("carouselPresignedUrls") ?: error("carouselPresignedUrls schema is missing")
    presignedUrls.get("type")?.asText() shouldBe "object"
    presignedUrls.get("additionalProperties")?.get("type")?.asText() shouldBe "string"

    val presignedUploads =
        properties.get("carouselPresignedUploads")
            ?: error("carouselPresignedUploads schema is missing")
    presignedUploads.get("type")?.asText() shouldBe "object"
    presignedUploads.get("additionalProperties")?.get("\$ref")?.asText() shouldBe
        "#/components/schemas/CarouselPresignedUploadResponse"

    val uploadSchema =
        schemas.get("CarouselPresignedUploadResponse")
            ?: error("CarouselPresignedUploadResponse schema is missing")
    val uploadProperties = uploadSchema.get("properties") ?: error("Upload properties are missing")
    uploadProperties.get("uploadUrl")?.get("type")?.asText() shouldBe "string"
    uploadProperties.get("imageKey")?.get("type")?.asText() shouldBe "string"
}

private fun assertParameterDescriptions(
    parameters: JsonNode?,
    context: String,
) {
    if (parameters == null || parameters.isNull) return
    check(parameters.isArray) { "$context.parameters must be an array" }
    parameters.forEachIndexed { index, parameter ->
        parameter.requireNonBlank("description", "$context parameter[$index]")
    }
}
