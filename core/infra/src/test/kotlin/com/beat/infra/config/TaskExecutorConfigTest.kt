package com.beat.infra.config

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

class TaskExecutorConfigTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration::class.java))
            .withUserConfiguration(TaskExecutorConfig::class.java)

    @Test
    fun `beat async executor is configured with thread pool properties`() {
        contextRunner
            .withPropertyValues(
                "thread-pool.core-size=3",
                "thread-pool.max-pool-size=5",
                "thread-pool.queue-capacity=20",
                "thread-pool.thread-name-prefix=test-executor-",
            ).run { context ->
                assertTrue(context.containsBean("applicationTaskExecutor"))
                val properties = context.getBean(ThreadPoolProperties::class.java)
                val executor = context.getBean("beatAsyncExecutor", ThreadPoolTaskExecutor::class.java)

                assertEquals(3, properties.coreSize)
                assertEquals(5, properties.maxPoolSize)
                assertEquals(20, properties.queueCapacity)
                assertEquals("test-executor-", properties.threadNamePrefix)
                assertEquals(3, executor.corePoolSize)
                assertEquals(5, executor.maxPoolSize)
                assertEquals(20, executor.queueCapacity)
                assertEquals("test-executor-", executor.threadNamePrefix)
            }
    }

    @Test
    fun `available task decorator beans are applied to beat async executor`() {
        val decorated = AtomicBoolean(false)
        val taskDecorator = TaskDecorator { runnable ->
            Runnable {
                decorated.set(true)
                runnable.run()
            }
        }

        contextRunner
            .withBean(TaskDecorator::class.java, { taskDecorator })
            .withPropertyValues(
                "thread-pool.core-size=1",
                "thread-pool.max-pool-size=1",
                "thread-pool.queue-capacity=1",
                "thread-pool.thread-name-prefix=test-executor-",
            ).run { context ->
                val executor = context.getBean("beatAsyncExecutor", ThreadPoolTaskExecutor::class.java)
                val latch = CountDownLatch(1)

                executor.execute(latch::countDown)

                assertTrue(latch.await(3, TimeUnit.SECONDS))
                assertTrue(decorated.get())
            }
    }

    @Test
    fun `beat async executor is excluded from default injection candidate`() {
        contextRunner
            .withPropertyValues(
                "thread-pool.core-size=3",
                "thread-pool.max-pool-size=5",
                "thread-pool.queue-capacity=20",
                "thread-pool.thread-name-prefix=test-executor-",
            ).run { context ->
                val defaultExecutor = context.getBean(Executor::class.java)
                val appExecutor = context.getBean("applicationTaskExecutor", ThreadPoolTaskExecutor::class.java)
                val beatExecutor = context.getBean("beatAsyncExecutor", ThreadPoolTaskExecutor::class.java)

                assertEquals(appExecutor, defaultExecutor)
                assertTrue(beatExecutor !== defaultExecutor)
            }
    }
}
