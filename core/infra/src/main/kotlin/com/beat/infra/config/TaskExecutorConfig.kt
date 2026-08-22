package com.beat.infra.config

import java.util.concurrent.ThreadPoolExecutor
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.core.task.support.CompositeTaskDecorator
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

// Spring Boot가 자동 구성하는 applicationTaskExecutor는 MVC/JPA/WebSocket 등 프레임워크 통합용으로 유지하고,
// 비즈니스 @Async 작업은 @Async("beatAsyncExecutor")로 명시해 별도 풀에서 실행한다.
// defaultCandidate=false를 통해 타입 기반 기본 주입 후보에서는 제외하고, 명시적 이름/qualifier로만 사용되게 한다.
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ThreadPoolProperties::class)
internal class TaskExecutorConfig {
    @Bean(name = ["beatAsyncExecutor"], defaultCandidate = false)
    fun beatAsyncExecutor(
        threadPoolProperties: ThreadPoolProperties,
        taskDecorators: ObjectProvider<TaskDecorator>,
    ): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            setCorePoolSize(threadPoolProperties.coreSize)
            setMaxPoolSize(maxOf(threadPoolProperties.maxPoolSize, threadPoolProperties.coreSize))
            setQueueCapacity(threadPoolProperties.queueCapacity)
            setThreadNamePrefix(threadPoolProperties.threadNamePrefix)
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(30)
            setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
            applyTaskDecorator(this, taskDecorators)
            initialize()
        }

    private fun applyTaskDecorator(
        executor: ThreadPoolTaskExecutor,
        taskDecorators: ObjectProvider<TaskDecorator>,
    ) {
        val decorators = taskDecorators.orderedStream().toList()
        when (decorators.size) {
            0 -> return
            1 -> executor.setTaskDecorator(decorators.first())
            else -> executor.setTaskDecorator(CompositeTaskDecorator(decorators))
        }
    }
}
