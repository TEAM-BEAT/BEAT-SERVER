package com.beat.apps.admin.openapi

import com.beat.apps.admin.support.BeatAdminAcceptanceTest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
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

        test("admin OpenAPI 문서를 생성하고 API 계약을 검증한다") {
            val result =
                mockMvc
                    .perform(get("/api/admin/v3/api-docs/admin"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.openapi").exists())
                    .andExpect(jsonPath("$.paths").exists())
                    .andReturn()

            val outputDirectory =
                Path.of(requireNotNull(System.getProperty("beat.openapi.output.dir")))
            Files.createDirectories(outputDirectory)
            Files.write(outputDirectory.resolve("admin.json"), result.response.contentAsByteArray)

            objectMapper.readTree(result.response.contentAsString).assertOpenApiContract()
        }
    }
}

private val OPEN_API_HTTP_METHODS =
    setOf("get", "post", "put", "patch", "delete", "options", "head", "trace")
private val SEMVER_PATTERN =
    Regex("""^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$""")

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
    val version = info.requireNonBlank("version", "info")
    check(SEMVER_PATTERN.matches(version)) { "info.version must be a SemVer version" }

    val paths = get("paths") ?: error("OpenAPI paths are missing")
    val operationIds = mutableSetOf<String>()
    val pathEntries = paths.fields()
    while (pathEntries.hasNext()) {
        val pathEntry = pathEntries.next()
        val path = pathEntry.key
        val pathItem = pathEntry.value
        val operations = pathItem.fields()
        while (operations.hasNext()) {
            val operationEntry = operations.next()
            if (operationEntry.key !in OPEN_API_HTTP_METHODS) continue

            val context = "${operationEntry.key.uppercase()} $path"
            val operation = operationEntry.value
            val operationId = operation.requireNonBlank("operationId", context)
            check(operationIds.add(operationId)) { "$context.operationId '$operationId' is duplicated" }
            operation.requireNonBlank("summary", context)

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

}
