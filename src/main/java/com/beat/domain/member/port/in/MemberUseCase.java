package com.beat.domain.member.port.in;

import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;

public interface MemberUseCase {
	Member findMemberByMemberId(Long memberId);

	boolean checkMemberExistsBySocialIdAndSocialType(Long socialId, SocialType socialType);

	Member findMemberBySocialIdAndSocialType(Long socialId, SocialType socialType);

	/**
 * Deletes a user identified by the given ID.
 *
 * @param id the unique identifier of the user to delete
 */
void deleteUser(Long id);

	/**
 * Returns the total number of members.
 *
 * @return the total member count
 */
long countMembers();
}
