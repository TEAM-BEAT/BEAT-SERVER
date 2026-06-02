package com.beat.infra.persistence.booking.repository.query

import com.beat.contracts.booking.MakerTicketReadPort
import com.beat.contracts.booking.readmodel.MakerTicketBookingStatus
import com.beat.contracts.booking.readmodel.MakerTicketListItemReadModel
import com.beat.contracts.booking.readmodel.MakerTicketScheduleNumber
import com.beat.domain.booking.domain.BookingStatus
import com.beat.domain.performance.domain.BankName
import com.beat.domain.schedule.domain.ScheduleNumber
import com.beat.infra.persistence.booking.entity.BookingJpaEntity
import com.beat.infra.persistence.schedule.entity.ScheduleJpaEntity
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.querymodel.jpql.select.SelectQuery
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.extension.createQuery
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
// JpqlRenderContext is provided by JpaConfig, which is wired via @EnableInfraBaseConfig's
// DeferredImportSelector. IntelliJ cannot statically trace that path, so the injection is flagged as a
// false positive; runtime wiring is verified by the module context-boot integration tests.
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class MakerTicketReadPortImpl(
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
) : MakerTicketReadPort {

    override fun findTickets(
        performanceId: Long?,
        scheduleNumbers: List<MakerTicketScheduleNumber>,
        bookingStatuses: List<MakerTicketBookingStatus>,
    ): List<MakerTicketListItemReadModel> {
        val query = buildTicketQuery(performanceId, scheduleNumbers, bookingStatuses, searchWord = null)
        return entityManager.createQuery(query, jpqlRenderContext).resultList.map(::toReadModel)
    }

    override fun searchTickets(
        performanceId: Long?,
        searchWord: String?,
        scheduleNumbers: List<MakerTicketScheduleNumber>,
        bookingStatuses: List<MakerTicketBookingStatus>,
    ): List<MakerTicketListItemReadModel> {
        if (searchWord.isNullOrBlank()) {
            return emptyList()
        }
        val query = buildTicketQuery(performanceId, scheduleNumbers, bookingStatuses, searchWord)
        return entityManager.createQuery(query, jpqlRenderContext).resultList.map(::toReadModel)
    }

    private fun buildTicketQuery(
        performanceId: Long?,
        scheduleNumberNames: List<MakerTicketScheduleNumber>,
        bookingStatusNames: List<MakerTicketBookingStatus>,
        searchWord: String?,
    ): SelectQuery<BookingJpaEntity> {
        val scheduleNumbers = scheduleNumberNames.map(::toScheduleNumber)
        val bookingStatuses = bookingStatusNames.map(::toBookingStatus)

        return jpql {
            select(
                entity(BookingJpaEntity::class),
            ).from(
                entity(BookingJpaEntity::class),
                entity(ScheduleJpaEntity::class),
            ).whereAnd(
                path(BookingJpaEntity::scheduleId).eq(path(ScheduleJpaEntity::id)),
                path(BookingJpaEntity::bookingStatus).ne(BookingStatus.BOOKING_DELETED),
                performanceId?.let { path(ScheduleJpaEntity::performanceId).eq(it) },
                scheduleNumbers.takeIf { it.isNotEmpty() }
                    ?.let { path(ScheduleJpaEntity::scheduleNumber).`in`(it) },
                bookingStatuses.takeIf { it.isNotEmpty() }
                    ?.let { path(BookingJpaEntity::bookingStatus).`in`(it) },
                // MySQL full-text search: function('match', bookerName, :searchWord) > 0
                searchWord?.let {
                    function(Double::class, "match", path(BookingJpaEntity::bookerName), value(it)).gt(0.0)
                },
            ).orderBy(
                // 상태 우선순위 -> 최신순. 기존 JPQL ORDER BY CASE ... END ASC, createdAt DESC 와 동일.
                caseWhen(path(BookingJpaEntity::bookingStatus).eq(BookingStatus.REFUND_REQUESTED)).then(1)
                    .`when`(path(BookingJpaEntity::bookingStatus).eq(BookingStatus.CHECKING_PAYMENT)).then(2)
                    .`when`(path(BookingJpaEntity::bookingStatus).eq(BookingStatus.BOOKING_CONFIRMED)).then(3)
                    .`when`(path(BookingJpaEntity::bookingStatus).eq(BookingStatus.BOOKING_CANCELLED)).then(4)
                    .`else`(5)
                    .asc(),
                path(BookingJpaEntity::createdAt).desc(),
            )
        }
    }

    private fun toReadModel(entity: BookingJpaEntity): MakerTicketListItemReadModel =
        MakerTicketListItemReadModel(
            entity.id,
            entity.bookerName,
            entity.bookerPhoneNumber,
            entity.scheduleId,
            entity.purchaseTicketCount,
            entity.createdAt,
            toMakerTicketBookingStatus(entity.bookingStatus),
            toBankName(entity.bankName),
            entity.accountNumber ?: "",
            entity.accountHolder ?: "",
        )

    private fun toBankName(bankName: BankName?): String =
        (bankName ?: BankName.NONE).displayName

    private fun toScheduleNumber(scheduleNumber: MakerTicketScheduleNumber): ScheduleNumber =
        when (scheduleNumber) {
            MakerTicketScheduleNumber.FIRST -> ScheduleNumber.FIRST
            MakerTicketScheduleNumber.SECOND -> ScheduleNumber.SECOND
            MakerTicketScheduleNumber.THIRD -> ScheduleNumber.THIRD
            MakerTicketScheduleNumber.FOURTH -> ScheduleNumber.FOURTH
            MakerTicketScheduleNumber.FIFTH -> ScheduleNumber.FIFTH
            MakerTicketScheduleNumber.SIXTH -> ScheduleNumber.SIXTH
            MakerTicketScheduleNumber.SEVENTH -> ScheduleNumber.SEVENTH
            MakerTicketScheduleNumber.EIGHTH -> ScheduleNumber.EIGHTH
            MakerTicketScheduleNumber.NINTH -> ScheduleNumber.NINTH
            MakerTicketScheduleNumber.TENTH -> ScheduleNumber.TENTH
        }

    private fun toBookingStatus(bookingStatus: MakerTicketBookingStatus): BookingStatus =
        when (bookingStatus) {
            MakerTicketBookingStatus.CHECKING_PAYMENT -> BookingStatus.CHECKING_PAYMENT
            MakerTicketBookingStatus.BOOKING_CONFIRMED -> BookingStatus.BOOKING_CONFIRMED
            MakerTicketBookingStatus.BOOKING_CANCELLED -> BookingStatus.BOOKING_CANCELLED
            MakerTicketBookingStatus.REFUND_REQUESTED -> BookingStatus.REFUND_REQUESTED
            MakerTicketBookingStatus.BOOKING_DELETED -> BookingStatus.BOOKING_DELETED
        }

    private fun toMakerTicketBookingStatus(bookingStatus: BookingStatus): MakerTicketBookingStatus =
        when (bookingStatus) {
            BookingStatus.CHECKING_PAYMENT -> MakerTicketBookingStatus.CHECKING_PAYMENT
            BookingStatus.BOOKING_CONFIRMED -> MakerTicketBookingStatus.BOOKING_CONFIRMED
            BookingStatus.BOOKING_CANCELLED -> MakerTicketBookingStatus.BOOKING_CANCELLED
            BookingStatus.REFUND_REQUESTED -> MakerTicketBookingStatus.REFUND_REQUESTED
            BookingStatus.BOOKING_DELETED -> MakerTicketBookingStatus.BOOKING_DELETED
        }
}
