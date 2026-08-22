package com.beat.observability.logging.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import org.slf4j.MDC

class MdcCoroutineContextTest : FunSpec() {

    init {
        afterTest { MDC.clear() }

        test("current context propagates MDC when coroutine switches dispatcher") {
            runBlocking {
                MDC.put("traceId", "trace-123")
                MDC.put("userId", "member-1")

                val deferred = async(Dispatchers.Default + MdcCoroutineContext.current()) {
                    delay(10)
                    MDC.get("traceId") to MDC.get("userId")
                }
                MDC.clear()

                deferred.await() shouldBe ("trace-123" to "member-1")
            }
        }

        test("with current context propagates MDC to nested dispatcher switch") {
            runBlocking {
                MDC.put("traceId", "trace-456")

                val traceId = MdcCoroutineContext.withCurrent {
                    withContext(Dispatchers.Default) {
                        delay(10)
                        MDC.get("traceId")
                    }
                }

                traceId shouldBe "trace-456"
                MDC.get("traceId") shouldBe "trace-456"
            }
        }

        test("MDC updates inside coroutine are not implicitly captured after suspension") {
            runBlocking {
                MDC.put("traceId", "trace-original")

                val updatedTraceId = withContext(Dispatchers.Default + MdcCoroutineContext.current()) {
                    MDC.put("traceId", "trace-updated")
                    delay(10)
                    MDC.get("traceId")
                }

                updatedTraceId shouldBe "trace-original"
                MDC.get("traceId") shouldBe "trace-original"
                MDC.get("missing").shouldBeNull()
            }
        }
    }
}
