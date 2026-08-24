package com.beat.infrastructure.persistence.member.repository

import com.beat.domain.member.model.SocialType
import com.beat.infrastructure.persistence.member.entity.MemberJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface MemberJpaRepository : JpaRepository<MemberJpaEntity, Long> {
    @Query("SELECT m FROM Member m WHERE m.socialId = :socialId AND m.socialType = :socialType")
    fun findBySocialTypeAndSocialId(
        @Param("socialId") socialId: Long?,
        @Param("socialType") socialType: SocialType,
    ): MemberJpaEntity?
}
