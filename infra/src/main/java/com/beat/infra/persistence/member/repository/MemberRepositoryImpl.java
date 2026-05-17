package com.beat.infra.persistence.member.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.infra.persistence.member.entity.MemberJpaEntity;
import com.beat.infra.persistence.member.mapper.MemberPersistenceMapper;

@Repository
public class MemberRepositoryImpl implements MemberRepository {

	private final MemberJpaRepository memberJpaRepository;
	private final MemberPersistenceMapper memberPersistenceMapper;

	public MemberRepositoryImpl(MemberJpaRepository memberJpaRepository,
		MemberPersistenceMapper memberPersistenceMapper) {
		this.memberJpaRepository = memberJpaRepository;
		this.memberPersistenceMapper = memberPersistenceMapper;
	}

	@Override
	public Optional<Member> findById(Long id) {
		return memberJpaRepository.findById(id).map(memberPersistenceMapper::toDomain);
	}

	@Override
	public Member save(Member member) {
		MemberJpaEntity entity = memberPersistenceMapper.toEntity(member);
		MemberJpaEntity savedEntity = memberJpaRepository.save(entity);
		return memberPersistenceMapper.toDomain(savedEntity);
	}

	@Override
	public Optional<Member> findBySocialTypeAndSocialId(Long socialId, SocialType socialType) {
		return memberJpaRepository.findBySocialTypeAndSocialId(socialId, socialType)
			.map(memberPersistenceMapper::toDomain);
	}

	@Override
	public long count() {
		return memberJpaRepository.count();
	}
}
