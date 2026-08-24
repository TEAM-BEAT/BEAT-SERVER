package com.beat.support.observability.logging

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import org.slf4j.MDC

class MdcTaskDecoratorTest : FunSpec() {

    private val decorator = MdcTaskDecorator()

    init {
        afterTest { MDC.clear() }

        test("decorated task에 부모의 MDC context를 복사한다") {
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

        test("부모 context가 없으면 task의 MDC를 비우고 실행 후 worker context를 복원한다") {
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
