package com.beat.infrastructure.persistence.user.repository

import com.beat.infrastructure.persistence.user.entity.UsersJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

internal interface UsersJpaRepository : JpaRepository<UsersJpaEntity, Long>
