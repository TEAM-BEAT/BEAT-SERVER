package com.beat.apis.performance.application.event

import com.beat.contracts.cdn.ImageCachePort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.scheduling.annotation.Async
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PerformancePosterChangedEventListener(
    private val imageCachePort: ImageCachePort,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("beatAsyncExecutor")
    fun preWarmPoster(event: PerformancePosterChangedEvent) {
        try {
            imageCachePort.preWarm(event.posterImage)
        } catch (exception: RuntimeException) {
            log.error(exception) { "Performance poster cache pre-warm failed" }
        }
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
