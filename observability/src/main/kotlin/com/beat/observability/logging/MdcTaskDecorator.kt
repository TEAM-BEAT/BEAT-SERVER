package com.beat.observability.logging

import org.slf4j.MDC
import org.springframework.core.task.TaskDecorator

class MdcTaskDecorator : TaskDecorator {

    override fun decorate(runnable: Runnable): Runnable {
        val parentContext = MDC.getCopyOfContextMap()
        return Runnable {
            val previousContext = MDC.getCopyOfContextMap()
            try {
                if (parentContext.isNullOrEmpty()) {
                    MDC.clear()
                } else {
                    MDC.setContextMap(parentContext)
                }
                runnable.run()
            } finally {
                if (previousContext.isNullOrEmpty()) {
                    MDC.clear()
                } else {
                    MDC.setContextMap(previousContext)
                }
            }
        }
    }
}
