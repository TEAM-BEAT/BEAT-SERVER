package com.beat.infrastructure.persistence.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import java.time.LocalDateTime
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
internal abstract class BaseTimeEntity {

    @field:CreatedDate
    @field:Column(updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @field:LastModifiedDate
    var updatedAt: LocalDateTime? = null
        protected set
}
