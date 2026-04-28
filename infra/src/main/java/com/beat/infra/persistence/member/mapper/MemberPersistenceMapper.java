package com.beat.infra.persistence.member.mapper;

import org.springframework.stereotype.Component;

import com.beat.domain.member.domain.Member;
import com.beat.infra.persistence.member.entity.MemberJpaEntity;

@Component
public class MemberPersistenceMapper {

	public Member toDomain(MemberJpaEntity entity) {
		return Member.rehydrate(
			entity.getId(),
			entity.getNickname(),
			entity.getEmail(),
			entity.getDeletedAt(),
			entity.getUserId(),
			entity.getSocialId(),
			entity.getSocialType()
		);
	}

	public MemberJpaEntity toEntity(Member domain) {
		return MemberJpaEntity.rehydrate(
			domain.getId(),
			domain.getNickname(),
			domain.getEmail(),
			domain.getDeletedAt(),
			domain.getUserId(),
			domain.getSocialId(),
			domain.getSocialType()
		);
	}
}
