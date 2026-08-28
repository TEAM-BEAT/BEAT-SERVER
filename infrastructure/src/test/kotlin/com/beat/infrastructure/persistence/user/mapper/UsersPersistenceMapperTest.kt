package com.beat.infrastructure.persistence.user.mapper

import com.beat.domain.user.model.Role
import com.beat.domain.user.model.Users
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UsersPersistenceMapperTest :
    FunSpec({
        val mapper = UsersPersistenceMapper()

        test("왕복 시 저장된 user role을 보존한다") {
            val users = Users.rehydrate(11L, Role.ADMIN)

            val roundTrip = mapper.toDomain(mapper.toEntity(users))

            roundTrip.id shouldBe 11L
            roundTrip.role shouldBe Role.ADMIN
        }

        test("toEntity는 신규 user의 생성 id를 null로 유지한다") {
            val users = Users.createWithRole(Role.MEMBER)

            val entity = mapper.toEntity(users)

            entity.id shouldBe null
            entity.role shouldBe Role.MEMBER
        }
    })
