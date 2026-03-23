package com.beat.domain.member.dao;

import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;

import java.util.Optional;

public interface MemberRepositoryCustom {
	Optional<Member> findBySocialTypeAndSocialId(final Long socialId, final SocialType socialType);

}
