package com.beat.domain.member.dao;

import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;

import feign.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

	@Query("SELECT u FROM Member u WHERE u.socialId = :socialId AND u.socialType = :socialType")
	Optional<Member> findBySocialTypeAndSocialId(@Param("socialId") Long socialId,
		@Param("socialType") SocialType socialType);

}
