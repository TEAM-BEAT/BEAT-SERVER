package com.beat.infra.persistence.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beat.domain.member.domain.SocialType;
import com.beat.infra.persistence.member.entity.MemberJpaEntity;

public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {
	@Query("SELECT m FROM Member m WHERE m.socialId = :socialId AND m.socialType = :socialType")
	Optional<MemberJpaEntity> findBySocialTypeAndSocialId(
		@Param("socialId") Long socialId,
		@Param("socialType") SocialType socialType
	);
}
