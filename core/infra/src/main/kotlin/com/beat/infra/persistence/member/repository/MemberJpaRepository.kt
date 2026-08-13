package com.beat.infra.persistence.member.repository

import com.beat.domain.member.model.SocialType
import com.beat.infra.persistence.member.entity.MemberJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface MemberJpaRepository : JpaRepository<MemberJpaEntity, Long> {
    @Query("SELECT m FROM Member m WHERE m.socialId = :socialId AND m.socialType = :socialType")
    fun findBySocialTypeAndSocialId(
        @Param("socialId") socialId: Long?,
        @Param("socialType") socialType: SocialType,
    ): Optional<MemberJpaEntity>
}
