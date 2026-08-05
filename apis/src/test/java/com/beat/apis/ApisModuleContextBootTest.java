package com.beat.apis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.beat.apis.support.AbstractIntegrationTest;
import com.beat.contracts.auth.refreshtoken.RefreshTokenPort;
import com.beat.contracts.auth.guest.GuestAccessThrottlePort;
import com.beat.contracts.auth.guest.GuestSessionPort;
import com.beat.contracts.auth.social.SocialLoginPort;
import com.beat.contracts.cdn.ImageCachePort;
import com.beat.contracts.notification.BookingNotificationPort;
import com.beat.contracts.notification.MemberNotificationPort;
import com.beat.contracts.sms.SmsPort;
import com.beat.contracts.storage.FileStoragePort;
import com.beat.infra.external.cdn.ImageCacheAdapter;
import com.beat.infra.external.notification.slack.SlackBookingNotificationAdapter;
import com.beat.infra.external.notification.slack.SlackMemberNotificationAdapter;
import com.beat.infra.external.notification.sms.CoolSmsAdapter;
import com.beat.infra.external.social.kakao.KakaoSocialLoginAdapter;
import com.beat.infra.external.storage.s3.S3FileStorageAdapter;
import com.beat.infra.redis.auth.guest.RedisGuestAccessThrottleAdapter;
import com.beat.infra.redis.auth.guest.RedisGuestSessionAdapter;
import com.beat.infra.redis.auth.refreshtoken.RedisRefreshTokenAdapter;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.models.GroupedOpenApi;

import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.domain.schedule.repository.ScheduleRepository;

@Tag("integration")
class ApisModuleContextBootTest extends AbstractIntegrationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@BeforeEach
	void setUpMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void contextLoadsWithoutBatchScheduler() {
		assertEquals(1, applicationContext.getBeansOfType(GroupedOpenApi.class).size());
		assertEquals(1, applicationContext.getBeansOfType(OpenAPI.class).size());
		assertTrue(applicationContext.containsBean("generalApi"));
		assertFalse(applicationContext.containsBean("adminApi"));
		assertFalse(applicationContext.containsBean("jobSchedulerService"));
		assertTrue(applicationContext.getBeansOfType(TaskScheduler.class).isEmpty());
		assertEquals(1, applicationContext.getBeansOfType(PerformanceRepository.class).size());
		assertEquals(1, applicationContext.getBeansOfType(PromotionRepository.class).size());
		assertEquals(1, applicationContext.getBeansOfType(ScheduleRepository.class).size());
	}

	@Test
	void registersSelectedInfraBeans() {
		assertSame(
			applicationContext.getBean(RedisRefreshTokenAdapter.class),
			applicationContext.getBean(RefreshTokenPort.class)
		);
		assertSame(
			applicationContext.getBean(RedisGuestSessionAdapter.class),
			applicationContext.getBean(GuestSessionPort.class)
		);
		assertSame(
			applicationContext.getBean(RedisGuestAccessThrottleAdapter.class),
			applicationContext.getBean(GuestAccessThrottlePort.class)
		);
		assertSame(
			applicationContext.getBean(KakaoSocialLoginAdapter.class),
			applicationContext.getBean(SocialLoginPort.class)
		);
		assertSame(
			applicationContext.getBean(SlackBookingNotificationAdapter.class),
			applicationContext.getBean(BookingNotificationPort.class)
		);
		assertSame(
			applicationContext.getBean(SlackMemberNotificationAdapter.class),
			applicationContext.getBean(MemberNotificationPort.class)
		);
		assertSame(
			applicationContext.getBean(CoolSmsAdapter.class),
			applicationContext.getBean(SmsPort.class)
		);
		assertSame(
			applicationContext.getBean(S3FileStorageAdapter.class),
			applicationContext.getBean(FileStoragePort.class)
		);
		assertSame(
			applicationContext.getBean(ImageCacheAdapter.class),
			applicationContext.getBean(ImageCachePort.class)
		);

		RedisScript<?> script =
			applicationContext.getBean("recordGuestAccessFailureScript", RedisScript.class);
		assertEquals(Long.class, script.getResultType());
		assertTrue(script.getScriptAsString().contains("redis.call('INCR'"));
	}

	@Test
	void servesGroupedSwaggerDocsForGeneralApis() throws Exception {
		mockMvc.perform(get("/v3/api-docs/general"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").exists())
			.andExpect(jsonPath("$.paths").exists());
	}
}
