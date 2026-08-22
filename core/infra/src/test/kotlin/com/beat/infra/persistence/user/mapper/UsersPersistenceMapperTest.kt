package com.beat.infra.persistence.user.mapper

import com.beat.domain.user.model.Role
import com.beat.domain.user.model.Users
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UsersPersistenceMapperTest : FunSpec({
    val mapper = UsersPersistenceMapper()

    test("roundTripPreservesPersistedUserRole") {
        val users = Users.rehydrate(11L, Role.ADMIN)

        val roundTrip = mapper.toDomain(mapper.toEntity(users))

        roundTrip.id shouldBe 11L
        roundTrip.role shouldBe Role.ADMIN
    }

    test("toEntityKeepsGeneratedIdNullForNewUser") {
        val users = Users.createWithRole(Role.MEMBER)

        val entity = mapper.toEntity(users)

        entity.id shouldBe null
        entity.role shouldBe Role.MEMBER
    }
})
