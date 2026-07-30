package com.beat.apis.ticket.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "beat.load-test",
    name = ["enabled"],
    havingValue = "true",
)
class TicketConfirmationLoadTestInfoContributor(
    @param:Value("\${beat.ticket.confirmation-sms.enabled:true}")
    private val ticketConfirmationSmsEnabled: Boolean,
) : InfoContributor {
    override fun contribute(builder: Info.Builder) {
        builder.withDetail(
            "ticketConfirmationLoadTest",
            mapOf(
                "enabled" to true,
                "ticketConfirmationSmsEnabled" to ticketConfirmationSmsEnabled,
            ),
        )
    }
}
