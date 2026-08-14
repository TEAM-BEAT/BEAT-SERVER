package com.beat.infra.persistence.user.mapper

import com.beat.domain.user.model.Users
import com.beat.infra.persistence.user.entity.UsersJpaEntity
import org.springframework.stereotype.Component

@Component
class UsersPersistenceMapper {
    fun toDomain(entity: UsersJpaEntity): Users =
        Users.rehydrate(entity.id, entity.role)

    fun toEntity(domain: Users): UsersJpaEntity =
        UsersJpaEntity.rehydrate(domain.getId(), domain.role)
}
