package com.beat.apis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.beat.contracts.schedule.ScheduleJobPort;
import com.beat.apis.support.AbstractIntegrationTest;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.models.GroupedOpenApi;

import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.domain.schedule.dao.ScheduleRepository;

@Tag("integration")
class ApisModuleContextBootTest extends AbstractIntegrationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ScheduleJobPort scheduleJobPort;

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@BeforeEach
	void setUpMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void contextLoadsWithModuleLocalNonOwnerScheduleJobPort() {
		assertNotNull(scheduleJobPort);
		assertEquals(1, applicationContext.getBeansOfType(ScheduleJobPort.class).size());
		assertEquals(1, applicationContext.getBeansOfType(GroupedOpenApi.class).size());
		assertEquals(1, applicationContext.getBeansOfType(OpenAPI.class).size());
		assertTrue(applicationContext.containsBean("generalApi"));
		assertFalse(applicationContext.containsBean("adminApi"));
		assertFalse(applicationContext.containsBean("jobSchedulerService"));
		assertTrue(applicationContext.getBeansOfType(TaskScheduler.class).isEmpty());
		assertFalse(scheduleJobPort.getClass().getName().contains("JobSchedulerService"));
		assertEquals(1, applicationContext.getBeansOfType(PerformanceRepository.class).size());
		assertEquals(1, applicationContext.getBeansOfType(PromotionRepository.class).size());
		assertEquals(1, applicationContext.getBeansOfType(ScheduleRepository.class).size());
	}

	@Test
	void servesGroupedSwaggerDocsForGeneralApis() throws Exception {
		mockMvc.perform(get("/v3/api-docs/general"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").exists())
			.andExpect(jsonPath("$.paths").exists());
	}
}
