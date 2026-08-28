package com.beat.infrastructure.persistence.booking.credential

import com.beat.application.frontoffice.booking.booker.command.credential.GuestBookingCredential
import com.beat.application.frontoffice.booking.booker.command.credential.GuestBookingCredentialStore
import com.beat.infrastructure.persistence.booking.entity.BookingJpaEntity
import com.beat.infrastructure.persistence.booking.repository.BookingJpaRepository
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.extension.createQuery
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
internal class GuestBookingCredentialJpaStore(
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
    private val bookingJpaRepository: BookingJpaRepository,
) : GuestBookingCredentialStore {

    override fun findCandidates(
        bookerName: String,
        phoneNumber: String,
        birthDate: String,
    ): List<GuestBookingCredential> {
        val query = jpql {
            selectDistinctNew<GuestCredentialProjection>(
                    path(BookingJpaEntity::userId),
                    path(BookingJpaEntity::password),
                )
                .from(entity(BookingJpaEntity::class))
                .whereAnd(
                    path(BookingJpaEntity::bookerName).eq(bookerName),
                    path(BookingJpaEntity::bookerPhoneNumber).eq(phoneNumber),
                    path(BookingJpaEntity::birthDate).eq(birthDate),
                    path(BookingJpaEntity::password).isNotNull(),
                )
        }

        return entityManager.createQuery(query, jpqlRenderContext).resultList.map { projection ->
            GuestBookingCredential(
                userId = projection.userId,
                encodedPassword = checkNotNull(projection.encodedPassword),
            )
        }
    }

    override fun replaceEncodedPassword(userId: Long, encodedPassword: String): Int =
        bookingJpaRepository.replaceGuestPassword(userId, encodedPassword)

    private data class GuestCredentialProjection(
        val userId: Long,
        val encodedPassword: String?,
    )
}
