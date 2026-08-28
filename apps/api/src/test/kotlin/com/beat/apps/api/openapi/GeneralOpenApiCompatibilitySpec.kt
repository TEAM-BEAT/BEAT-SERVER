package com.beat.apps.api.openapi

import com.beat.apps.api.performance.api.type.BankNameType
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

        test("general OpenAPI 문서를 생성하고 API 계약을 검증한다") {
            val result =
                mockMvc
                    .perform(get("/v3/api-docs/general"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.openapi").exists())
                    .andExpect(jsonPath("$.paths").exists())
                    .andReturn()

            val outputDirectory =
                Path.of(requireNotNull(System.getProperty("beat.openapi.output.dir")))
            Files.createDirectories(outputDirectory)
            Files.write(outputDirectory.resolve("general.json"), result.response.contentAsByteArray)

            objectMapper.readTree(result.response.contentAsString).assertOpenApiContract()
        }
    }
}

private val OPEN_API_HTTP_METHODS =
    setOf("get", "post", "put", "patch", "delete", "options", "head", "trace")
private val SEMVER_PATTERN =
    Regex(
        """^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$"""
    )

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
            check(operationIds.add(operationId)) {
                "$context.operationId '$operationId' is duplicated"
            }
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

    assertBookingRefundRequestContract()
    assertGuestBookingOperationsContract()
}

private fun JsonNode.assertBookingRefundRequestContract() {
    val schema = at("/components/schemas/BookingRefundRequest")
    check(!schema.isMissingNode) { "BookingRefundRequest schema is missing" }

    val requiredFields = schema.get("required")?.map { it.asText() }.orEmpty()
    val accountFields = listOf("bankName", "accountNumber", "accountHolder")
    check(requiredFields.containsAll(accountFields)) {
        "BookingRefundRequest must require ${accountFields.joinToString()}"
    }

    val acceptedBankNames =
        BankNameType.entries.filterNot { it == BankNameType.NONE }.map { it.name }
    schema.at("/properties/bankName/type").asText() shouldBe "string"
    schema.at("/properties/bankName/enum").map { it.asText() } shouldBe acceptedBankNames
    accountFields.drop(1).forEach { fieldName ->
        schema.at("/properties/$fieldName/type").asText() shouldBe "string"
    }
}

private fun JsonNode.assertGuestBookingOperationsContract() {
    assertOperationContract(
        path = "/api/bookings/guest/retrieve",
        method = "post",
        operationId = "getGuestBookings",
        responseCodes = setOf("200", "400", "404", "429"),
        requestSchema = "GuestBookingRetrieveRequest",
    )
    assertOperationContract(
        path = "/api/bookings/refund",
        method = "patch",
        operationId = "requestBookingRefund",
        responseCodes = setOf("200", "400", "401", "403", "404", "409"),
        requestSchema = "BookingRefundRequest",
    )
    assertOperationContract(
        path = "/api/bookings/cancel",
        method = "patch",
        operationId = "requestBookingCancellation",
        responseCodes = setOf("200", "401", "403", "404", "409"),
        requestSchema = "BookingCancelRequest",
    )
}

private fun JsonNode.assertOperationContract(
    path: String,
    method: String,
    operationId: String,
    responseCodes: Set<String>,
    requestSchema: String,
) {
    val operation = get("paths")?.get(path)?.get(method)
    check(operation != null) { "${method.uppercase()} $path operation is missing" }
    operation.requireNonBlank("operationId", "${method.uppercase()} $path") shouldBe operationId
    operation.at("/requestBody/required").asBoolean() shouldBe true
    operation.at("/requestBody/content/application~1json/schema/\$ref").asText() shouldBe
        "#/components/schemas/$requestSchema"
    operation.get("responses")?.fieldNames()?.asSequence()?.toSet() shouldBe responseCodes
}
