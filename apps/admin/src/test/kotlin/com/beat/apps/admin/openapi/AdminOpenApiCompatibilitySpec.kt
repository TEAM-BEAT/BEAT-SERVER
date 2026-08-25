package com.beat.apps.admin.openapi

import com.beat.apps.admin.support.BeatAdminAcceptanceTest
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

    init {
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("admin OpenAPI 호환성 문서를 생성한다") {
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
        }
    }
}
