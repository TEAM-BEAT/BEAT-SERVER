package com.beat.application.frontoffice.member.command

import com.beat.domain.member.repository.MemberRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
internal class MemberRegisteredEventListener(
    private val memberRepository: MemberRepository,
    private val memberRegistrationNotifier: MemberRegistrationNotifier,
) {
    @Async("beatAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendSlackNotification(event: MemberRegisteredEvent) {
        try {
            memberRegistrationNotifier.send(
                MemberRegistrationNotification(
                    nickname = event.nickname,
                    memberCount = memberRepository.count(),
                )
            )
        } catch (exception: RuntimeException) {
            log.error(exception) {
                "Member registration Slack notification failed: errorType=${exception.javaClass.simpleName}"
            }
        }
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
