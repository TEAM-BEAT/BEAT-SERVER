package com.beat.infra.persistence.booking.repository

import com.beat.application.frontoffice.booking.booker.credential.GuestBookingCredential
import com.beat.application.frontoffice.booking.booker.credential.GuestBookingCredentialRepository
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.infra.config.JpaConfig
import com.beat.infra.persistence.booking.entity.BookingJpaEntity
import com.beat.infra.support.MySqlTestContainerConfig
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest(
    properties = [
        "spring.config.import=classpath:application-persistence.yml",
        "DB_HIKARI_MAX_POOL_SIZE=10",
    ],
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = [JpaConfig::class, MySqlTestContainerConfig::class])
@ActiveProfiles("test")
@Tags("integration")
class BookingRepositoryQueriesIntegrationSpec : FunSpec() {

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var bookingJpaRepository: BookingJpaRepository

    @Autowired
    private lateinit var guestBookingCredentialRepository: GuestBookingCredentialRepository

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("returns schedule ids for the selected booking ids from MySQL") {
            val first = bookingJpaRepository.saveAndFlush(booking(scheduleId = 101L, userId = 1L))
            val second = bookingJpaRepository.saveAndFlush(booking(scheduleId = 202L, userId = 2L))
            val third = bookingJpaRepository.saveAndFlush(booking(scheduleId = 303L, userId = 3L))

            bookingRepository.findScheduleIdsByIds(listOf(checkNotNull(first.id), checkNotNull(third.id)))
                .shouldContainExactlyInAnyOrder(101L, 303L)
        }

        test("returns an empty result when no booking ids are requested") {
            bookingRepository.findScheduleIdsByIds(emptyList()).shouldBeEmpty()
        }

        test("returns distinct non-null guest credential projections for the matching identity") {
            bookingJpaRepository.saveAllAndFlush(
                listOf(
                    booking(
                        scheduleId = 401L,
                        userId = 11L,
                        bookerName = TARGET_NAME,
                        phoneNumber = TARGET_PHONE,
                        birthDate = TARGET_BIRTH_DATE,
                        password = "hash-a",
                    ),
                    booking(
                        scheduleId = 402L,
                        userId = 11L,
                        bookerName = TARGET_NAME,
                        phoneNumber = TARGET_PHONE,
                        birthDate = TARGET_BIRTH_DATE,
                        password = "hash-a",
                    ),
                    booking(
                        scheduleId = 403L,
                        userId = 11L,
                        bookerName = TARGET_NAME,
                        phoneNumber = TARGET_PHONE,
                        birthDate = TARGET_BIRTH_DATE,
                        password = "hash-b",
                    ),
                    booking(
                        scheduleId = 404L,
                        userId = 22L,
                        bookerName = TARGET_NAME,
                        phoneNumber = TARGET_PHONE,
                        birthDate = TARGET_BIRTH_DATE,
                        password = "hash-c",
                    ),
                    booking(
                        scheduleId = 405L,
                        userId = 33L,
                        bookerName = TARGET_NAME,
                        phoneNumber = TARGET_PHONE,
                        birthDate = TARGET_BIRTH_DATE,
                        password = null,
                    ),
                    booking(
                        scheduleId = 406L,
                        userId = 44L,
                        bookerName = "other-booker",
                        phoneNumber = TARGET_PHONE,
                        birthDate = TARGET_BIRTH_DATE,
                        password = "mismatched-name",
                    ),
                ),
            )

            guestBookingCredentialRepository.findCandidates(TARGET_NAME, TARGET_PHONE, TARGET_BIRTH_DATE)
                .shouldContainExactlyInAnyOrder(
                    GuestBookingCredential(11L, "hash-a"),
                    GuestBookingCredential(11L, "hash-b"),
                    GuestBookingCredential(22L, "hash-c"),
                )
        }
    }

    private fun booking(
        scheduleId: Long,
        userId: Long,
        bookerName: String = "booker",
        phoneNumber: String = "010-0000-0000",
        birthDate: String? = null,
        password: String? = null,
    ): BookingJpaEntity =
        BookingJpaEntity.rehydrate(
            id = null,
            purchaseTicketCount = 1,
            bookerName = bookerName,
            bookerPhoneNumber = phoneNumber,
            bookingStatus = BookingStatus.CHECKING_PAYMENT,
            createdAt = LocalDateTime.of(2026, 8, 22, 12, 0),
            cancellationDate = null,
            birthDate = birthDate,
            password = password,
            refundAccount = null,
            scheduleId = scheduleId,
            userId = userId,
            totalPaymentAmount = 10_000,
        )

    private companion object {
        const val TARGET_NAME = "target-booker"
        const val TARGET_PHONE = "010-1111-1111"
        const val TARGET_BIRTH_DATE = "900101"
    }
}
