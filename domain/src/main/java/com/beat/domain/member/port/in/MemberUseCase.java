package com.beat.domain.member.port.in;

import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;

public interface MemberUseCase {
	Member findMemberByMemberId(Long memberId);

	boolean checkMemberExistsBySocialIdAndSocialType(Long socialId, SocialType socialType);

	Member findMemberBySocialIdAndSocialType(Long socialId, SocialType socialType);

	void deleteUser(Long id);

	long countMembers();
}
