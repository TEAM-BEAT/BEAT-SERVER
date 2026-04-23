package com.beat.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.beat.admin.port.in.AdminUseCase;
import com.beat.admin.support.AbstractAdminIntegrationTest;
import com.beat.contracts.schedule.ScheduleJobPort;
import com.beat.contracts.storage.FileStoragePort;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.models.GroupedOpenApi;

import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.promotion.repository.PromotionRepository;

class AdminModuleContextBootTest extends AbstractAdminIntegrationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@BeforeEach
	void setUpMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@MockitoBean
	private FileStoragePort fileStoragePort;

	@Test
	void contextLoads() {
		assertEquals(1, applicationContext.getBeansOfType(AdminUseCase.class).size());
		assertEquals(1, applicationContext.getBeansOfType(GroupedOpenApi.class).size());
		assertEquals(1, applicationContext.getBeansOfType(OpenAPI.class).size());
		assertTrue(applicationContext.containsBean("adminApi"));
		assertFalse(applicationContext.containsBean("jobSchedulerService"));
		assertTrue(applicationContext.getBeansOfType(ScheduleJobPort.class).isEmpty());
		assertTrue(applicationContext.getBeansOfType(TaskScheduler.class).isEmpty());
		assertEquals(1, applicationContext.getBeansOfType(PerformanceRepository.class).size());
		assertEquals(1, applicationContext.getBeansOfType(PromotionRepository.class).size());
	}

	@Test
	void servesGroupedSwaggerDocsForAdminApis() throws Exception {
		mockMvc.perform(get("/v3/api-docs/admin"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").exists())
			.andExpect(jsonPath("$.paths").exists());
	}
}
