package com.beat.infra.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class TaskExecutorConfigTest : FunSpec({
    val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration::class.java))
            .withUserConfiguration(TaskExecutorConfig::class.java)

    test("beat async executor는 thread pool 속성으로 구성된다") {
        contextRunner
            .withPropertyValues(
                "thread-pool.core-size=3",
                "thread-pool.max-pool-size=5",
                "thread-pool.queue-capacity=20",
                "thread-pool.thread-name-prefix=test-executor-",
            ).run { context ->
                context.containsBean("applicationTaskExecutor") shouldBe true
                val properties = context.getBean(ThreadPoolProperties::class.java)
                val executor = context.getBean("beatAsyncExecutor", ThreadPoolTaskExecutor::class.java)

                properties.coreSize shouldBe 3
                properties.maxPoolSize shouldBe 5
                properties.queueCapacity shouldBe 20
                properties.threadNamePrefix shouldBe "test-executor-"
                executor.corePoolSize shouldBe 3
                executor.maxPoolSize shouldBe 5
                executor.queueCapacity shouldBe 20
                executor.threadNamePrefix shouldBe "test-executor-"
            }
    }

    test("등록된 task decorator bean이 beat async executor에 적용된다") {
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

                latch.await(3, TimeUnit.SECONDS) shouldBe true
                decorated.get() shouldBe true
            }
    }

    test("beat async executor는 기본 주입 후보에서 제외된다") {
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

                defaultExecutor shouldBe appExecutor
                (beatExecutor !== defaultExecutor) shouldBe true
            }
    }
})
