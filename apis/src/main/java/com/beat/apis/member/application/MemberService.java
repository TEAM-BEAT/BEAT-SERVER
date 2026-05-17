package com.beat.apis.member.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.member.application.exception.MemberApplicationErrorCode;
import com.beat.apis.member.application.result.MemberAuthenticationResult;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.support.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class MemberService {
	private final UserRepository userRepository;
	private final MemberRepository memberRepository;

	@Transactional(readOnly = true)
	public MemberAuthenticationResult findMemberAuthenticationResultByMemberId(Long memberId) {
		return memberRepository.findById(memberId)
			.map(this::toAuthenticationResult)
			.orElseThrow(() -> new NotFoundException(MemberApplicationErrorCode.MEMBER_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public boolean checkMemberExistsBySocialIdAndSocialType(final Long socialId, final SocialType socialType) {
		return memberRepository.findBySocialTypeAndSocialId(socialId, socialType).isPresent();
	}

	@Transactional(readOnly = true)
	public MemberAuthenticationResult findMemberAuthenticationResultBySocialIdAndSocialType(final Long socialId,
		final SocialType socialType) {
		return memberRepository.findBySocialTypeAndSocialId(socialId, socialType)
			.map(this::toAuthenticationResult)
			.orElseThrow(() -> new NotFoundException(MemberApplicationErrorCode.MEMBER_NOT_FOUND));
	}

	@Transactional
	public void deleteUser(final Long id) {
		Users users = userRepository.findById(id)
			.orElseThrow(() -> new NotFoundException(MemberApplicationErrorCode.MEMBER_NOT_FOUND));

		userRepository.delete(users);
	}

	@Transactional(readOnly = true)
	public long countMembers() {
		return memberRepository.count();
	}

	private MemberAuthenticationResult toAuthenticationResult(Member member) {
		return MemberAuthenticationResult.of(member.getId(), member.getUserId());
	}
}
