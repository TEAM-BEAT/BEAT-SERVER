package com.beat.infra.persistence.member.mapper

import com.beat.domain.exception.DomainException
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.vo.SocialIdentity
import com.beat.infra.persistence.exception.PersistenceMappingException
import com.beat.infra.persistence.member.entity.MemberJpaEntity
import org.springframework.stereotype.Component

@Component
class MemberPersistenceMapper {
    fun toDomain(entity: MemberJpaEntity): Member =
        try {
            val socialType: SocialType? = entity.socialType
            checkNotNull(socialType) { "Stored Member socialType is null" }
            Member.rehydrate(
                entity.id,
                entity.nickname,
                entity.email,
                entity.deletedAt,
                entity.userId,
                SocialIdentity.of(socialType, entity.socialId),
            )
        } catch (exception: DomainException) {
            throw PersistenceMappingException.invalidStoredState("Member", entity.id, exception)
        } catch (exception: IllegalStateException) {
            throw PersistenceMappingException.invalidStoredState("Member", entity.id, exception)
        }

    fun toEntity(domain: Member): MemberJpaEntity =
        MemberJpaEntity.rehydrate(
            domain.getId(),
            domain.nickname,
            domain.email,
            domain.deletedAt,
            domain.getUserId(),
            domain.socialIdentity.socialId,
            domain.socialIdentity.socialType,
        )
}
