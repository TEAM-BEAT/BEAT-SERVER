package com.beat.observability.logging.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class MdcCoroutineContextTest {

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    @Test
    fun `current context propagates MDC when coroutine switches dispatcher`() = runBlocking {
        MDC.put("traceId", "trace-123")
        MDC.put("userId", "member-1")

        val deferred = async(Dispatchers.Default + MdcCoroutineContext.current()) {
            delay(10)
            MDC.get("traceId") to MDC.get("userId")
        }
        MDC.clear()

        assertEquals("trace-123" to "member-1", deferred.await())
    }

    @Test
    fun `with current context propagates MDC to nested dispatcher switch`() = runBlocking {
        MDC.put("traceId", "trace-456")

        val traceId = MdcCoroutineContext.withCurrent {
            withContext(Dispatchers.Default) {
                delay(10)
                MDC.get("traceId")
            }
        }

        assertEquals("trace-456", traceId)
        assertEquals("trace-456", MDC.get("traceId"))
    }

    @Test
    fun `MDC updates inside coroutine are not implicitly captured after suspension`() = runBlocking {
        MDC.put("traceId", "trace-original")

        val updatedTraceId = withContext(Dispatchers.Default + MdcCoroutineContext.current()) {
            MDC.put("traceId", "trace-updated")
            delay(10)
            MDC.get("traceId")
        }

        assertEquals("trace-original", updatedTraceId)
        assertEquals("trace-original", MDC.get("traceId"))
        assertNull(MDC.get("missing"))
    }
}
