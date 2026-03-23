package com.beat.admin;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;

import com.beat.admin.port.in.AdminUseCase;
import com.beat.contracts.storage.FileStoragePort;
import com.beat.domain.member.port.in.MemberUseCase;
import com.beat.domain.performance.port.in.PerformanceUseCase;
import com.beat.domain.promotion.port.in.PromotionUseCase;
import com.beat.domain.user.port.in.UserUseCase;
import com.redis.testcontainers.RedisContainer;

@SpringBootTest(classes = AdminApplication.class)
@ActiveProfiles("test")
@Tag("integration")
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

	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.39")
		.withDatabaseName("beat_admin_test");

	@ServiceConnection
	static RedisContainer redis =
		new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG));

	static {
		mysql.start();
		redis.start();
	}

	@Test
	void contextLoads() {
	}
}
