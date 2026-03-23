package com.beat.admin;

import com.redis.testcontainers.RedisContainer;
import com.beat.admin.port.in.AdminUseCase;
import com.beat.contracts.storage.FileStoragePort;
import com.beat.domain.member.port.in.MemberUseCase;
import com.beat.domain.performance.port.in.PerformanceUseCase;
import com.beat.domain.promotion.port.in.PromotionUseCase;
import com.beat.domain.user.port.in.UserUseCase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = AdminApplication.class)
@ActiveProfiles("test")
@Tag("integration")
@Testcontainers
class AdminModuleContextBootTest {

	@MockitoBean
	private AdminUseCase adminUseCase;

	@MockitoBean
	private PromotionUseCase promotionUseCase;

	@MockitoBean
	private PerformanceUseCase performanceUseCase;

	@MockitoBean
	private FileStoragePort fileStoragePort;

	@MockitoBean
	private MemberUseCase memberUseCase;

	@MockitoBean
	private UserUseCase userUseCase;

	@Container
	private static final RedisContainer REDIS_CONTAINER =
		new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG));

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
		registry.add("spring.data.redis.port", REDIS_CONTAINER::getFirstMappedPort);
	}

	@Test
	void contextLoads() {
	}
}
