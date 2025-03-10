package com.beat.domain.member.application;

import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.member.port.in.MemberUseCase;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Users;
import com.beat.global.common.exception.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class MemberService implements MemberUseCase {
	private final UserRepository userRepository;
	private final MemberRepository memberRepository;

	@Override
	@Transactional(readOnly = true)
	public Member findMemberByMemberId(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean checkMemberExistsBySocialIdAndSocialType(final Long socialId, final SocialType socialType) {
		return memberRepository.findBySocialTypeAndSocialId(socialId, socialType).isPresent();
	}

	/**
	 * Retrieves a member using the specified social ID and social type.
	 * <p>
	 * Searches the member repository for a member matching the provided social ID and social type.
	 * If no such member exists, a NotFoundException with error code MEMBER_NOT_FOUND is thrown.
	 *
	 * @param socialId the unique social identifier of the member
	 * @param socialType the social platform type associated with the member
	 * @return the member corresponding to the given social credentials
	 * @throws NotFoundException if no matching member is found
	 */
	@Override
	@Transactional(readOnly = true)
	public Member findMemberBySocialIdAndSocialType(final Long socialId, final SocialType socialType) {
		return memberRepository.findBySocialTypeAndSocialId(socialId, socialType)
			.orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	/**
	 * Deletes a user identified by the specified ID.
	 *
	 * <p>This method retrieves the user from the repository and removes it. If no user is found with the
	 * given ID, a {@link NotFoundException} is thrown with the error code {@link MemberErrorCode#MEMBER_NOT_FOUND}.
	 *
	 * @param id the unique identifier of the user to delete
	 * @throws NotFoundException if no user with the specified ID is found
	 */
	@Override
	@Transactional
	public void deleteUser(final Long id) {
		Users users = userRepository.findById(id)
			.orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

		userRepository.delete(users);
	}

	/**
	 * Retrieves the total count of members.
	 *
	 * @return the total number of members
	 */
	@Override
	@Transactional(readOnly = true)
	public long countMembers() {
		return memberRepository.count();
	}
}
