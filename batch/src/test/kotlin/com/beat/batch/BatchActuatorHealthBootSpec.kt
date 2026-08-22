package com.beat.batch

import com.beat.batch.support.BeatBatchAcceptanceTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.web.server.LocalManagementPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@BeatBatchAcceptanceTest
@Tags("acceptance")
class BatchActuatorHealthBootSpec : FunSpec() {
    @LocalManagementPort
    private var managementPort: Int = 0

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("actuator health endpoint를 기존 management path로 제공한다") {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:$managementPort/actuator-test/health"))
                .GET()
                .build()

            val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())

            response.statusCode() shouldBe 200
            response.body() shouldContain "\"status\""
        }
    }
}
