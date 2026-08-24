package com.beat.infrastructure.config

import io.kotest.core.NamedTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@ContextConfiguration(
    classes = [TaskExecutionAutoConfiguration::class, TaskExecutorConfig::class],
)
@TestPropertySource(
    properties = [
        "thread-pool.core-size=3",
        "thread-pool.max-pool-size=5",
        "thread-pool.queue-capacity=20",
        "thread-pool.thread-name-prefix=test-executor-",
    ],
)
@Tags("integration")
open class KotestSpringCoexistencePilotSpec : FunSpec() {
    @Autowired
    private lateinit var threadPoolProperties: ThreadPoolProperties

    @Autowired
    @Qualifier("beatAsyncExecutor")
    private lateinit var beatAsyncExecutor: ThreadPoolTaskExecutor

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        context("Spring TestContext가 Kotest leaf lifecycle로 실행되면").config(tags = setOf(INTEGRATION)) {
            test("BEAT async executor 설정을 주입한다") {
                threadPoolProperties.coreSize shouldBe 3
                threadPoolProperties.maxPoolSize shouldBe 5
                threadPoolProperties.queueCapacity shouldBe 20
                threadPoolProperties.threadNamePrefix shouldBe "test-executor-"
                beatAsyncExecutor.corePoolSize shouldBe 3
                beatAsyncExecutor.maxPoolSize shouldBe 5
                beatAsyncExecutor.queueCapacity shouldBe 20
                beatAsyncExecutor.threadNamePrefix shouldBe "test-executor-"
            }
        }
    }
}

private val INTEGRATION = NamedTag("integration")
