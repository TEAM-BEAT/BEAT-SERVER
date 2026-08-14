package com.beat.infra.persistence.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTimeEntity {

    @field:CreatedDate
    @field:Column(updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @field:LastModifiedDate
    var updatedAt: LocalDateTime? = null
        protected set
}
