package com.beat.apps.api.file.api

import com.beat.apps.api.file.facade.FileFacade
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.convention.TestBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [FileController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [FileController::class])
class FileControllerWebSpec : FunSpec() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @TestBean
    private lateinit var fileFacade: FileFacade

    init {
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        beforeTest {
            clearMocks(fileFacade)
        }

        test("canonical performanceImages 파라미터가 전달된다") {
            mockMvc.perform(
                get("/api/files/presigned-url")
                    .param("posterImage", "poster.png")
                    .param("performanceImages", "performance.png"),
            ).andExpect(status().isOk)

            verify {
                fileFacade.issueAllPresignedUrlsForPerformanceMaker(
                    "poster.png",
                    null,
                    null,
                    listOf("performance.png"),
                )
            }
        }

        test("빈 performanceImages 값은 빈 리스트로 바인딩된다") {
            mockMvc.perform(
                get("/api/files/presigned-url")
                    .param("posterImage", "poster.png")
                    .param("performanceImages", ""),
            ).andExpect(status().isOk)

            verify {
                fileFacade.issueAllPresignedUrlsForPerformanceMaker(
                    "poster.png",
                    null,
                    null,
                    emptyList(),
                )
            }
        }
    }

    private companion object {
        @JvmStatic
        fun fileFacade(): FileFacade = mockk(relaxed = true)
    }
}
