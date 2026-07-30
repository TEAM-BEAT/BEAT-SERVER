package com.beat.apis.ticket.application.event

import com.beat.apis.ticket.config.TicketConfirmationLoadTestInfoContributor
import com.beat.contracts.sms.SmsPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

class TicketPaymentConfirmedEventListenerConditionTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(TestConfig::class.java)

    @Test
    fun `설정이 없으면 예매 확정 SMS listener를 등록한다`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(TicketPaymentConfirmedEventListener::class.java)
        }
    }

    @Test
    fun `SMS가 비활성화되면 예매 확정 SMS listener를 등록하지 않는다`() {
        contextRunner
            .withPropertyValues("beat.ticket.confirmation-sms.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(TicketPaymentConfirmedEventListener::class.java)
            }
    }

    @Test
    fun `load test가 활성화되면 SMS 비활성 상태를 info에 노출한다`() {
        contextRunner
            .withPropertyValues(
                "beat.load-test.enabled=true",
                "beat.ticket.confirmation-sms.enabled=false",
            ).run { context ->
                val contributor = context.getBean(TicketConfirmationLoadTestInfoContributor::class.java)
                val infoBuilder = Info.Builder()

                contributor.contribute(infoBuilder)

                assertThat(infoBuilder.build().details["ticketConfirmationLoadTest"])
                    .isEqualTo(
                        mapOf(
                            "enabled" to true,
                            "ticketConfirmationSmsEnabled" to false,
                        ),
                    )
            }
    }

    @Test
    fun `load test가 비활성화되면 marker를 등록하지 않는다`() {
        contextRunner.run { context ->
            assertThat(context).doesNotHaveBean(TicketConfirmationLoadTestInfoContributor::class.java)
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(
        TicketPaymentConfirmedEventListener::class,
        TicketConfirmationLoadTestInfoContributor::class,
    )
    class TestConfig {
        @Bean
        fun smsPort(): SmsPort = SmsPort {}
    }
}
