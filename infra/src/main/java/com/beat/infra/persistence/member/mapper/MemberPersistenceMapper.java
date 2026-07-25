package com.beat.infra.persistence.member.mapper;

import org.springframework.stereotype.Component;

import com.beat.domain.exception.DomainException;
import com.beat.domain.member.model.Member;
import com.beat.domain.member.model.SocialType;
import com.beat.domain.member.vo.SocialIdentity;
import com.beat.infra.persistence.exception.PersistenceMappingException;
import com.beat.infra.persistence.member.entity.MemberJpaEntity;

@Component
public class MemberPersistenceMapper {

	public Member toDomain(MemberJpaEntity entity) {
		try {
			SocialType socialType = entity.getSocialType();
			if (socialType == null) {
				throw new IllegalStateException("Stored Member socialType is null");
			}
			return Member.rehydrate(
				entity.getId(),
				entity.getNickname(),
				entity.getEmail(),
				entity.getDeletedAt(),
				entity.getUserId(),
				SocialIdentity.of(socialType, entity.getSocialId())
			);
		} catch (DomainException | IllegalStateException exception) {
			throw PersistenceMappingException.invalidStoredState("Member", entity.getId(), exception);
		}
	}

	public MemberJpaEntity toEntity(Member domain) {
		return MemberJpaEntity.rehydrate(
			domain.getId(),
			domain.getNickname(),
			domain.getEmail(),
			domain.getDeletedAt(),
			domain.getUserId(),
			domain.getSocialIdentity().getSocialId(),
			domain.getSocialIdentity().getSocialType()
		);
	}
}
