package com.beat.apis.file.api

import com.beat.apis.file.facade.FileFacade
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [FileController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [FileController::class])
class FileControllerWebSpec : FunSpec() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var fileFacade: FileFacade

    init {
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        beforeTest {
            Mockito.reset(fileFacade)
        }

        test("canonical performanceImages parameter is forwarded") {
            mockMvc.perform(
                get("/api/files/presigned-url")
                    .param("posterImage", "poster.png")
                    .param("performanceImages", "performance.png"),
            ).andExpect(status().isOk)

            Mockito.verify(fileFacade).issueAllPresignedUrlsForPerformanceMaker(
                "poster.png",
                null,
                null,
                listOf("performance.png"),
            )
        }

        test("legacy performImages parameter is forwarded") {
            mockMvc.perform(
                get("/api/files/presigned-url")
                    .param("posterImage", "poster.png")
                    .param("performImages", "performance.png"),
            ).andExpect(status().isOk)

            Mockito.verify(fileFacade).issueAllPresignedUrlsForPerformanceMaker(
                "poster.png",
                null,
                null,
                listOf("performance.png"),
            )
        }

        test("canonical performanceImages takes precedence over legacy alias") {
            mockMvc.perform(
                get("/api/files/presigned-url")
                    .param("posterImage", "poster.png")
                    .param("performanceImages", "canonical.png")
                    .param("performImages", "legacy.png"),
            ).andExpect(status().isOk)

            Mockito.verify(fileFacade).issueAllPresignedUrlsForPerformanceMaker(
                "poster.png",
                null,
                null,
                listOf("canonical.png"),
            )
        }

        test("empty performanceImages placeholder is bound as an empty list") {
            mockMvc.perform(
                get("/api/files/presigned-url")
                    .param("posterImage", "poster.png")
                    .param("performanceImages", ""),
            ).andExpect(status().isOk)

            Mockito.verify(fileFacade).issueAllPresignedUrlsForPerformanceMaker(
                "poster.png",
                null,
                null,
                emptyList(),
            )
        }
    }
}
