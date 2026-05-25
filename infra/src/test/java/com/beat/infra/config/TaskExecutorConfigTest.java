package com.beat.infra.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class TaskExecutorConfigTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
		.withUserConfiguration(TaskExecutorConfig.class);

	@Test
	void customizerOverridesAutoConfiguredApplicationTaskExecutorWithThreadPoolProperties() {
		contextRunner
			.withPropertyValues(
				"thread-pool.core-size=3",
				"thread-pool.max-pool-size=5",
				"thread-pool.queue-capacity=20",
				"thread-pool.thread-name-prefix=test-executor-"
			)
			.run(context -> {
				ThreadPoolProperties properties = context.getBean(ThreadPoolProperties.class);
				ThreadPoolTaskExecutor executor = context.getBean(
					"applicationTaskExecutor",
					ThreadPoolTaskExecutor.class
				);

				assertEquals(3, properties.getCoreSize());
				assertEquals(5, properties.getMaxPoolSize());
				assertEquals(20, properties.getQueueCapacity());
				assertEquals("test-executor-", properties.getThreadNamePrefix());

				assertEquals(3, executor.getCorePoolSize());
				assertEquals(5, executor.getMaxPoolSize());
				assertEquals(20, executor.getQueueCapacity());
				assertEquals("test-executor-", executor.getThreadNamePrefix());
			});
	}

	@Test
	void appliesAvailableTaskDecoratorBeansToApplicationExecutor() {
		AtomicBoolean decorated = new AtomicBoolean(false);
		TaskDecorator taskDecorator = runnable -> () -> {
			decorated.set(true);
			runnable.run();
		};

		contextRunner
			.withBean(TaskDecorator.class, () -> taskDecorator)
			.withPropertyValues(
				"thread-pool.core-size=1",
				"thread-pool.max-pool-size=1",
				"thread-pool.queue-capacity=1",
				"thread-pool.thread-name-prefix=test-executor-"
			)
			.run(context -> {
				ThreadPoolTaskExecutor executor = context.getBean(
					"applicationTaskExecutor",
					ThreadPoolTaskExecutor.class
				);
				CountDownLatch latch = new CountDownLatch(1);

				executor.execute(latch::countDown);

				assertTrue(latch.await(3, TimeUnit.SECONDS));
				assertTrue(decorated.get());
			});
	}
}
