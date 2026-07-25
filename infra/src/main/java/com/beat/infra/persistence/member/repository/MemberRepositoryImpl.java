package com.beat.infra.persistence.member.repository;

import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.beat.domain.member.model.Member;
import com.beat.domain.member.vo.SocialIdentity;
import com.beat.domain.member.exception.DuplicateSocialIdentityException;
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
		try {
			MemberJpaEntity savedEntity = memberJpaRepository.saveAndFlush(entity);
			return memberPersistenceMapper.toDomain(savedEntity);
		} catch (DataIntegrityViolationException exception) {
			if (hasConstraint(exception, "uk_member_social_identity")) {
				throw new DuplicateSocialIdentityException(exception);
			}
			throw exception;
		}
	}

	private boolean hasConstraint(Throwable exception, String expectedConstraintName) {
		String expectedIdentifier = normalizeConstraintName(expectedConstraintName);
		if (expectedIdentifier == null) {
			return false;
		}

		Throwable cause = exception;
		while (cause != null) {
			if (cause instanceof ConstraintViolationException constraintViolation) {
				String actualIdentifier = normalizeConstraintName(constraintViolation.getConstraintName());
				if (expectedIdentifier.equalsIgnoreCase(actualIdentifier)) {
					return true;
				}
			}
			cause = cause.getCause();
		}
		return false;
	}

	static String normalizeConstraintName(String constraintName) {
		if (constraintName == null) {
			return null;
		}

		String identifier = constraintName.trim();
		int qualifierSeparator = identifier.lastIndexOf('.');
		if (qualifierSeparator >= 0) {
			identifier = identifier.substring(qualifierSeparator + 1).trim();
		}

		int start = 0;
		int end = identifier.length();
		while (start < end && isIdentifierQuote(identifier.charAt(start))) {
			start++;
		}
		while (end > start && isIdentifierQuote(identifier.charAt(end - 1))) {
			end--;
		}

		String normalized = identifier.substring(start, end).trim();
		return normalized.isEmpty() ? null : normalized;
	}

	private static boolean isIdentifierQuote(char character) {
		return character == '`' || character == '\'' || character == '"';
	}

	@Override
	public Optional<Member> findBySocialIdentity(SocialIdentity socialIdentity) {
		return memberJpaRepository.findBySocialTypeAndSocialId(
			socialIdentity.getSocialId(),
			socialIdentity.getSocialType()
		)
			.map(memberPersistenceMapper::toDomain);
	}

	@Override
	public long count() {
		return memberJpaRepository.count();
	}
}
