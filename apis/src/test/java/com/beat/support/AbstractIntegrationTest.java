package com.beat.support;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared integration-test base for the legacy root lane.
 *
 * <p>MySQL is started through the Testcontainers JDBC driver in {@code application-test.yml},
 * while Redis is managed here through the JUnit 5 Testcontainers lifecycle so Spring can receive
 * the dynamic host and port via {@link DynamicPropertySource}.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
@Testcontainers
public abstract class AbstractIntegrationTest {

	@Container
	private static final RedisContainer REDIS_CONTAINER =
		new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG));

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
		registry.add("spring.data.redis.port", REDIS_CONTAINER::getFirstMappedPort);
	}
}
