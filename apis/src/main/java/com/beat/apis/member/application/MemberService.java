package com.beat.apis.member.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.common.exception.NotFoundException;

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
	public Member findMemberByMemberId(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public boolean checkMemberExistsBySocialIdAndSocialType(final Long socialId, final SocialType socialType) {
		return memberRepository.findBySocialTypeAndSocialId(socialId, socialType).isPresent();
	}

	@Transactional(readOnly = true)
	public Member findMemberBySocialIdAndSocialType(final Long socialId, final SocialType socialType) {
		return memberRepository.findBySocialTypeAndSocialId(socialId, socialType)
			.orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	@Transactional
	public void deleteUser(final Long id) {
		Users users = userRepository.findById(id)
			.orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

		userRepository.delete(users);
	}

	@Transactional(readOnly = true)
	public long countMembers() {
		return memberRepository.count();
	}
}
