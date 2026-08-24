package com.beat.apps.admin

import com.beat.apps.admin.support.BeatAdminAcceptanceTest
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.promotion.repository.PromotionRepository
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import io.swagger.v3.oas.models.OpenAPI
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.TaskScheduler
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@BeatAdminAcceptanceTest
@Tags("acceptance")
class AdminModuleContextBootSpec : FunSpec() {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var mockMvc: MockMvc

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("admin 구성과 persistence 인프라만 부팅한다") {
            val groupedOpenApis = applicationContext.getBeansOfType(GroupedOpenApi::class.java).values
            groupedOpenApis.size shouldBe 1
            groupedOpenApis.single().group shouldBe "admin"
            applicationContext.getBeansOfType(OpenAPI::class.java).size shouldBe 1
            applicationContext.getBeansOfType(TaskScheduler::class.java).size shouldBe 0
            applicationContext.getBeansOfType(PerformanceRepository::class.java).size shouldBe 1
            applicationContext.getBeansOfType(PromotionRepository::class.java).size shouldBe 1

            applicationContext.containsBean("jobSchedulerService") shouldBe false
            applicationContext.containsBean("redisConnectionFactory") shouldBe false
            applicationContext.containsBean("refreshTokenRedisRepository") shouldBe false
            applicationContext.containsBean("guestSessionRedisRepository") shouldBe false
            applicationContext.containsBean("redisRefreshTokenAdapter") shouldBe false
            applicationContext.containsBean("redisGuestSessionAdapter") shouldBe false
            applicationContext.containsBean("redisGuestAccessThrottleAdapter") shouldBe false
        }

        test("prod 외 환경에서 그룹화된 admin OpenAPI 문서를 제공한다") {
            mockMvc.perform(get("/api/admin/v3/api-docs/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths").exists())
        }
    }
}
