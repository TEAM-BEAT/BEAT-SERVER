package com.beat.domain.member.application;

import com.beat.domain.member.application.dto.event.MemberRegisteredEvent;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Role;
import com.beat.domain.user.domain.Users;
import com.beat.global.auth.client.dto.MemberInfoResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberRegistrationService {

	private final ApplicationEventPublisher eventPublisher;
	private final UserRepository userRepository;
	private final MemberRepository memberRepository;

	/**
	 * Registers a new member and its associated user account.
	 *
	 * <p>This method creates a user with the MEMBER role and a corresponding member record using the
	 * provided member information. It saves both entities transactionally and publishes a registration event
	 * with the member's nickname.
	 *
	 * @param memberInfoResponse the data required for member registration, including nickname, email, social ID, and social type
	 * @return the identifier of the newly registered member
	 */
	@Transactional
	public Long registerMemberWithUserInfo(final MemberInfoResponse memberInfoResponse) {
		Users users = Users.createWithRole(Role.MEMBER);

		log.info("Granting MEMBER role to new user with role: {}", users.getRole());

		users = userRepository.save(users);
		userRepository.flush();

		log.info("Registering new user with role: {}", users.getRole());

		Member member = Member.create(
			memberInfoResponse.nickname(),
			memberInfoResponse.email(),
			users,
			memberInfoResponse.socialId(),
			memberInfoResponse.socialType()
		);

		memberRepository.save(member);
		log.info("Member registered with memberId: {}, role: {}", member.getId(), users.getRole());

		eventPublisher.publishEvent(new MemberRegisteredEvent(member.getNickname()));

		return member.getId();
	}
}
