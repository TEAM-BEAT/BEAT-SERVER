package com.beat.apps.api.openapi

import com.beat.apps.api.support.BeatAcceptanceTest
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

@BeatAcceptanceTest
@Tags("openapi")
class GeneralOpenApiCompatibilitySpec : FunSpec() {

    @Autowired private lateinit var mockMvc: MockMvc
    private val objectMapper = jacksonObjectMapper()

    init {
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("general OpenAPI 문서를 생성하고 필수 설명 계약을 검증한다") {
            val result =
                mockMvc
                    .perform(get("/v3/api-docs/general"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.openapi").exists())
                    .andExpect(jsonPath("$.paths").exists())
                    .andReturn()

            objectMapper.readTree(result.response.contentAsString).assertOpenApiContract()

            val outputDirectory =
                Path.of(requireNotNull(System.getProperty("beat.openapi.output.dir")))
            Files.createDirectories(outputDirectory)
            Files.write(outputDirectory.resolve("general.json"), result.response.contentAsByteArray)
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
