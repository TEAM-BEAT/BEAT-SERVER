package com.beat.observability.logging

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator

@Configuration(proxyBeanMethods = false)
class LoggingConfig {

    @Bean
    fun mdcTaskDecorator(): TaskDecorator = MdcTaskDecorator()
}
