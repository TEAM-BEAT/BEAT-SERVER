package com.beat.infrastructure.persistence.ticket.repository.query

import com.beat.application.frontoffice.ticket.maker.query.MakerTicketBookingStatus
import com.beat.application.frontoffice.ticket.maker.query.MakerTicketListItemReadModel
import com.beat.application.frontoffice.ticket.maker.query.MakerTicketReader
import com.beat.application.frontoffice.ticket.maker.query.MakerTicketScheduleNumber
import com.beat.application.frontoffice.ticket.maker.query.MakerTicketScheduleReadModel
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.infrastructure.persistence.booking.entity.BookingJpaEntity
import com.beat.infrastructure.persistence.booking.entity.RefundAccountJpaValue
import com.beat.infrastructure.persistence.schedule.entity.ScheduleJpaEntity
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.querymodel.jpql.select.SelectQuery
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.extension.createQuery
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
internal class MakerTicketQueries(
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
) : MakerTicketReader {

    override fun findTickets(
        performanceId: Long,
        scheduleNumbers: List<MakerTicketScheduleNumber>,
        bookingStatuses: List<MakerTicketBookingStatus>,
    ): List<MakerTicketListItemReadModel> =
        queryTickets(performanceId, scheduleNumbers, bookingStatuses, searchWord = null)

    override fun searchTickets(
        performanceId: Long,
        searchWord: String,
        scheduleNumbers: List<MakerTicketScheduleNumber>,
        bookingStatuses: List<MakerTicketBookingStatus>,
    ): List<MakerTicketListItemReadModel> {
        if (searchWord.isBlank()) {
            return emptyList()
        }
        return queryTickets(performanceId, scheduleNumbers, bookingStatuses, searchWord)
    }

    override fun findSchedules(performanceId: Long): List<MakerTicketScheduleReadModel> {
        val query = jpql {
            selectNew<MakerTicketScheduleProjection>(
                path(ScheduleJpaEntity::id),
                path(ScheduleJpaEntity::totalTicketCount),
                path(ScheduleJpaEntity::soldTicketCount),
                path(ScheduleJpaEntity::scheduleNumber),
            ).from(
                entity(ScheduleJpaEntity::class),
            ).where(
                path(ScheduleJpaEntity::performanceId).eq(performanceId),
            )
        }

        return entityManager.createQuery(query, jpqlRenderContext).resultList.map { projection ->
            MakerTicketScheduleReadModel(
                scheduleId = checkNotNull(projection.scheduleId),
                totalTicketCount = projection.totalTicketCount,
                soldTicketCount = projection.soldTicketCount,
                scheduleNumber = projection.scheduleNumber.name,
            )
        }
    }

    private fun queryTickets(
        performanceId: Long,
        scheduleNumbers: List<MakerTicketScheduleNumber>,
        bookingStatuses: List<MakerTicketBookingStatus>,
        searchWord: String?,
    ): List<MakerTicketListItemReadModel> =
        entityManager.createQuery(
            buildTicketQuery(performanceId, scheduleNumbers, bookingStatuses, searchWord),
            jpqlRenderContext,
        ).resultList.map(::toReadModel)

    private fun buildTicketQuery(
        performanceId: Long,
        scheduleNumberNames: List<MakerTicketScheduleNumber>,
        bookingStatusNames: List<MakerTicketBookingStatus>,
        searchWord: String?,
    ): SelectQuery<MakerTicketProjection> {
        val scheduleNumbers = scheduleNumberNames.map(::toScheduleNumber)
        val bookingStatuses = bookingStatusNames.map(::toBookingStatus)
        return jpql {
            val refundAccount = path(BookingJpaEntity::refundAccount)
            selectNew<MakerTicketProjection>(
                path(BookingJpaEntity::id),
                path(BookingJpaEntity::bookerName),
                path(BookingJpaEntity::bookerPhoneNumber),
                path(BookingJpaEntity::scheduleId),
                path(BookingJpaEntity::purchaseTicketCount),
                path(BookingJpaEntity::createdAt),
                path(BookingJpaEntity::bookingStatus),
                path(BookingJpaEntity::totalPaymentAmount),
                refundAccount(RefundAccountJpaValue::bankName),
                refundAccount(RefundAccountJpaValue::accountNumber),
                refundAccount(RefundAccountJpaValue::accountHolder),
            ).from(
                entity(BookingJpaEntity::class),
                join(entity(ScheduleJpaEntity::class)).on(
                    path(BookingJpaEntity::scheduleId).eq(path(ScheduleJpaEntity::id)),
                ),
            ).whereAnd(
                path(BookingJpaEntity::bookingStatus).ne(BookingStatus.BOOKING_DELETED),
                path(ScheduleJpaEntity::performanceId).eq(performanceId),
                scheduleNumbers.takeIf { it.isNotEmpty() }
                    ?.let { path(ScheduleJpaEntity::scheduleNumber).`in`(it) },
                bookingStatuses.takeIf { it.isNotEmpty() }
                    ?.let { path(BookingJpaEntity::bookingStatus).`in`(it) },
                searchWord?.let {
                    function(Double::class, "match", path(BookingJpaEntity::bookerName), value(it)).gt(0.0)
                },
            ).orderBy(
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

    private fun toReadModel(projection: MakerTicketProjection): MakerTicketListItemReadModel =
        MakerTicketListItemReadModel(
            bookingId = checkNotNull(projection.bookingId),
            bookerName = projection.bookerName,
            bookerPhoneNumber = projection.bookerPhoneNumber,
            scheduleId = projection.scheduleId,
            purchaseTicketCount = projection.purchaseTicketCount,
            createdAt = projection.createdAt,
            bookingStatus = toMakerTicketBookingStatus(projection.bookingStatus),
            bankName = (projection.bankName ?: BankName.NONE).displayName,
            accountNumber = projection.accountNumber ?: "",
            accountHolder = projection.accountHolder ?: "",
            deletable = Booking.canDeleteByMaker(projection.bookingStatus, projection.totalPaymentAmount),
        )

    private fun toScheduleNumber(scheduleNumber: MakerTicketScheduleNumber): ScheduleNumber =
        ScheduleNumber.valueOf(scheduleNumber.name)

    private fun toBookingStatus(bookingStatus: MakerTicketBookingStatus): BookingStatus =
        BookingStatus.valueOf(bookingStatus.name)

    private fun toMakerTicketBookingStatus(bookingStatus: BookingStatus): MakerTicketBookingStatus =
        MakerTicketBookingStatus.valueOf(bookingStatus.name)

    private data class MakerTicketProjection(
        val bookingId: Long?,
        val bookerName: String,
        val bookerPhoneNumber: String,
        val scheduleId: Long,
        val purchaseTicketCount: Int,
        val createdAt: LocalDateTime,
        val bookingStatus: BookingStatus,
        val totalPaymentAmount: Int?,
        val bankName: BankName?,
        val accountNumber: String?,
        val accountHolder: String?,
    )

    private data class MakerTicketScheduleProjection(
        val scheduleId: Long?,
        val totalTicketCount: Int,
        val soldTicketCount: Int,
        val scheduleNumber: ScheduleNumber,
    )
}
