package com.beat.infra.persistence.booking.repository.query

import com.beat.contracts.auth.guest.GuestCredentialReadPort
import com.beat.contracts.auth.guest.readmodel.GuestCredentialReadModel
import com.beat.infra.persistence.booking.entity.BookingJpaEntity
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.extension.createQuery
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
class GuestCredentialQueries(
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
) : GuestCredentialReadPort {

    override fun findCandidates(
        bookerName: String,
        phoneNumber: String,
        birthDate: String,
    ): List<GuestCredentialReadModel> {
        val query = jpql {
            selectDistinctNew<GuestCredentialProjection>(
                path(BookingJpaEntity::userId),
                path(BookingJpaEntity::password),
            ).from(
                entity(BookingJpaEntity::class),
            ).whereAnd(
                path(BookingJpaEntity::bookerName).eq(bookerName),
                path(BookingJpaEntity::bookerPhoneNumber).eq(phoneNumber),
                path(BookingJpaEntity::birthDate).eq(birthDate),
                path(BookingJpaEntity::password).isNotNull(),
            )
        }

        return entityManager.createQuery(query, jpqlRenderContext).resultList.map { projection ->
            GuestCredentialReadModel(
                userId = projection.userId,
                encodedPassword = checkNotNull(projection.encodedPassword),
            )
        }
    }

    private data class GuestCredentialProjection(
        val userId: Long,
        val encodedPassword: String?,
    )
}
