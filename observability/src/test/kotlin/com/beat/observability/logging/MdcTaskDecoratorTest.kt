package com.beat.observability.logging

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import org.slf4j.MDC

class MdcTaskDecoratorTest : FunSpec() {

    private val decorator = MdcTaskDecorator()

    init {
        afterTest { MDC.clear() }

        test("copies parent MDC context into decorated task") {
            MDC.put("traceId", "trace-123")
            MDC.put("userId", "member-1")

            val decorated = decorator.decorate(
                Runnable {
                    MDC.get("traceId") shouldBe "trace-123"
                    MDC.get("userId") shouldBe "member-1"
                    MDC.get("workerOnly").shouldBeNull()
                },
            )

            MDC.clear()
            MDC.put("workerOnly", "keep-me")

            decorated.run()

            MDC.get("traceId").shouldBeNull()
            MDC.get("userId").shouldBeNull()
            MDC.get("workerOnly") shouldBe "keep-me"
        }

        test("clears task MDC when parent has no context and restores worker context after execution") {
            MDC.clear()
            val decorated = decorator.decorate(
                Runnable {
                    MDC.getCopyOfContextMap().isNullOrEmpty() shouldBe true
                },
            )

            MDC.put("workerOnly", "keep-me")

            decorated.run()

            MDC.get("workerOnly") shouldBe "keep-me"
        }
    }
}
