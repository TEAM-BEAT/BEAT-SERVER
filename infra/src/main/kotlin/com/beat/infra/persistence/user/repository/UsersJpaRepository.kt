package com.beat.infra.persistence.user.repository

import com.beat.infra.persistence.user.entity.UsersJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UsersJpaRepository : JpaRepository<UsersJpaEntity, Long>
