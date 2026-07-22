package com.beat.apis.member.application.event

import com.beat.domain.member.repository.MemberRepository
import com.beat.contracts.notification.MemberNotification
import com.beat.contracts.notification.MemberNotificationPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class MemberRegisteredEventListener(
    private val memberRepository: MemberRepository,
    private val memberNotificationPort: MemberNotificationPort,
) {
    @Async("beatAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendSlackNotification(event: MemberRegisteredEvent) {
        try {
            memberNotificationPort.send(
                MemberNotification(
                    nickname = event.nickname,
                    memberCount = memberRepository.count(),
                ),
            )
        } catch (exception: RuntimeException) {
            log.error(exception) { "Member registration Slack notification failed: errorType=${exception.javaClass.simpleName}" }
        }
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
