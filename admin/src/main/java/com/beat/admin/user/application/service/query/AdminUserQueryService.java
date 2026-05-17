package com.beat.admin.user.application.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.admin.application.exception.AdminApplicationErrorCode;
import com.beat.admin.user.application.dto.response.UserFindAllResponse;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.support.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserQueryService {

	private final MemberRepository memberRepository;
	private final UserRepository userRepository;

	public UserFindAllResponse findAllUsers(Long memberId) {
		validateMemberExists(memberId);
		List<UserFindAllResponse.UserFindResponse> userResponses = userRepository.findAll().stream()
			.map(this::toUserFindResponse)
			.toList();
		return UserFindAllResponse.from(userResponses);
	}

	private UserFindAllResponse.UserFindResponse toUserFindResponse(Users user) {
		return UserFindAllResponse.UserFindResponse.of(
			user.getId(),
			user.getRole().getRoleName()
		);
	}

	private void validateMemberExists(Long memberId) {
		memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(AdminApplicationErrorCode.MEMBER_NOT_FOUND));
	}
}
