package com.beat.apis;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = ApisApplication.class)
@ActiveProfiles("test")
@Tag("integration")
@Testcontainers
class ApisModuleContextBootTest {

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
