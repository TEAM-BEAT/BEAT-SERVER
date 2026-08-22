package com.beat.infra.external.cdn

import com.beat.application.frontoffice.performance.maker.command.PerformancePosterChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
internal class PerformancePosterChangedEventListener(
    private val imageCacheAdapter: ImageCacheAdapter,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("beatAsyncExecutor")
    fun preWarmPoster(event: PerformancePosterChangedEvent) {
        try {
            imageCacheAdapter.preWarm(event.posterImage)
        } catch (exception: RuntimeException) {
            log.error("Performance poster cache pre-warm failed", exception)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(PerformancePosterChangedEventListener::class.java)
    }
}
