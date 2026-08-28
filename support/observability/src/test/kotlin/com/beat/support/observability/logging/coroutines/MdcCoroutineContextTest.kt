package com.beat.support.observability.logging.coroutines

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.MDC

class MdcCoroutineContextTest : FunSpec() {

    init {
        afterTest { MDC.clear() }

        test("coroutine이 dispatcher를 전환해도 current context가 MDC를 전파한다") {
            runBlocking {
                MDC.put("traceId", "trace-123")
                MDC.put("userId", "member-1")

                val deferred =
                    async(Dispatchers.Default + MdcCoroutineContext.current()) {
                        delay(10)
                        MDC.get("traceId") to MDC.get("userId")
                    }
                MDC.clear()

                deferred.await() shouldBe ("trace-123" to "member-1")
            }
        }

        test("withCurrent는 중첩된 dispatcher 전환에도 MDC를 전파한다") {
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

        test("suspension 이후 coroutine 내부의 MDC 변경은 암묵적으로 capture되지 않는다") {
            runBlocking {
                MDC.put("traceId", "trace-original")

                val updatedTraceId =
                    withContext(Dispatchers.Default + MdcCoroutineContext.current()) {
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
