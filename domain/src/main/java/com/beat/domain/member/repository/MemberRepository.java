package com.beat.domain.member.repository;

import java.util.Optional;

import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;

public interface MemberRepository {
	Optional<Member> findById(Long id);

	Member save(Member member);

	Optional<Member> findBySocialTypeAndSocialId(Long socialId, SocialType socialType);

	long count();
}
