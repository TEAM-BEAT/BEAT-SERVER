package com.beat.infrastructure.persistence.user.mapper

import com.beat.domain.user.model.Users
import com.beat.infrastructure.persistence.user.entity.UsersJpaEntity
import org.springframework.stereotype.Component

@Component
internal class UsersPersistenceMapper {
    fun toDomain(entity: UsersJpaEntity): Users =
        Users.rehydrate(entity.id, entity.role)

    fun toEntity(domain: Users): UsersJpaEntity =
        UsersJpaEntity.rehydrate(domain.id, domain.role)
}
