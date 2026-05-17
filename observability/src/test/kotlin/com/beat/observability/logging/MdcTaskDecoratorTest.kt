package com.beat.observability.logging

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class MdcTaskDecoratorTest {

    private val decorator = MdcTaskDecorator()

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    @Test
    fun `copies parent MDC context into decorated task`() {
        MDC.put("traceId", "trace-123")
        MDC.put("userId", "member-1")

        val decorated = decorator.decorate(
            Runnable {
                assertEquals("trace-123", MDC.get("traceId"))
                assertEquals("member-1", MDC.get("userId"))
                assertNull(MDC.get("workerOnly"))
            },
        )

        MDC.clear()
        MDC.put("workerOnly", "keep-me")

        decorated.run()

        assertNull(MDC.get("traceId"))
        assertNull(MDC.get("userId"))
        assertEquals("keep-me", MDC.get("workerOnly"))
    }

    @Test
    fun `clears task MDC when parent has no context and restores worker context after execution`() {
        MDC.clear()
        val decorated = decorator.decorate(
            Runnable {
                assertTrue(MDC.getCopyOfContextMap().isNullOrEmpty())
            },
        )

        MDC.put("workerOnly", "keep-me")

        decorated.run()

        assertEquals("keep-me", MDC.get("workerOnly"))
    }
}
