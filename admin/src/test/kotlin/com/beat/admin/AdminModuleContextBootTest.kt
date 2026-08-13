package com.beat.admin

import com.beat.admin.promotion.api.AdminPromotionController
import com.beat.admin.promotion.application.command.AdminPromotionCommandService
import com.beat.admin.promotion.application.query.AdminPromotionQueryService
import com.beat.admin.promotion.facade.AdminPromotionFacade
import com.beat.admin.support.AbstractAdminIntegrationTest
import com.beat.admin.user.api.AdminUserController
import com.beat.admin.user.application.query.AdminUserQueryService
import com.beat.admin.user.facade.AdminUserFacade
import com.beat.contracts.storage.FileStoragePort
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.promotion.repository.PromotionRepository
import io.swagger.v3.oas.models.OpenAPI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.TaskScheduler
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

class AdminModuleContextBootTest : AbstractAdminIntegrationTest() {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var fileStoragePort: FileStoragePort

    @BeforeEach
    fun setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun contextLoads() {
        assertEquals(1, applicationContext.getBeansOfType(GroupedOpenApi::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(OpenAPI::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(AdminUserController::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(AdminPromotionController::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(AdminUserFacade::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(AdminPromotionFacade::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(AdminUserQueryService::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(AdminPromotionQueryService::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(AdminPromotionCommandService::class.java).size)
        assertFalse(applicationContext.containsBean("jobSchedulerService"))
        assertTrue(applicationContext.getBeansOfType(TaskScheduler::class.java).isEmpty())
        assertFalse(applicationContext.containsBean("redisConnectionFactory"))
        assertFalse(applicationContext.containsBean("refreshTokenRedisRepository"))
        assertFalse(applicationContext.containsBean("guestSessionRedisRepository"))
        assertFalse(applicationContext.containsBean("redisRefreshTokenAdapter"))
        assertFalse(applicationContext.containsBean("redisGuestSessionAdapter"))
        assertFalse(applicationContext.containsBean("redisGuestAccessThrottleAdapter"))
        assertEquals(1, applicationContext.getBeansOfType(PerformanceRepository::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(PromotionRepository::class.java).size)
    }

    @Test
    fun servesGroupedSwaggerDocsForAdminApis() {
        mockMvc.perform(get("/api/admin/v3/api-docs/admin"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi").exists())
            .andExpect(jsonPath("$.paths").exists())
    }
}