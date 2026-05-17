package com.beat.apis.member.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.member.application.dto.event.MemberRegisteredEvent;
import com.beat.contracts.auth.social.SocialMemberInfo;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.user.domain.Role;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberRegistrationService {

	private final ApplicationEventPublisher eventPublisher;
	private final UserRepository userRepository;
	private final MemberRepository memberRepository;

	@Transactional
	public Long registerMemberWithUserInfo(final SocialMemberInfo socialMemberInfo, final SocialType socialType) {
		Users users = Users.createWithRole(Role.MEMBER);

		log.info("Granting MEMBER role to new user with role: {}", users.getRole());

		users = userRepository.save(users);

		log.info("Registering new user with role: {}", users.getRole());

		Member member = Member.create(
			socialMemberInfo.nickname(),
			socialMemberInfo.email(),
			users.getId(),
			socialMemberInfo.socialId(),
			socialType
		);

		Member savedMember = memberRepository.save(member);
		log.info("Member registered with memberId: {}, role: {}", savedMember.getId(), users.getRole());

		eventPublisher.publishEvent(new MemberRegisteredEvent(savedMember.getNickname()));

		return savedMember.getId();
	}
}
