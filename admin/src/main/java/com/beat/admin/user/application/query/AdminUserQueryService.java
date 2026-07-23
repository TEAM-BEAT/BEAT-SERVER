package com.beat.admin.user.application.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.admin.exception.AdminApplicationException;
import com.beat.admin.user.application.result.AdminUserResults;
import com.beat.admin.user.application.result.AdminUserResults.AdminUserResult;
import com.beat.admin.user.exception.UserApplicationErrorCode;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.user.model.Users;
import com.beat.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserQueryService {

	private final MemberRepository memberRepository;
	private final UserRepository userRepository;

	public AdminUserResults findAllUsers(Long memberId) {
		validateMemberExists(memberId);
		List<AdminUserResult> users = userRepository.findAll().stream()
			.map(this::toUserResult)
			.toList();
		return new AdminUserResults(users);
	}

	private AdminUserResult toUserResult(Users user) {
		return new AdminUserResult(
			user.getId(),
			user.getRole().getRoleName()
		);
	}

	private void validateMemberExists(Long memberId) {
		memberRepository.findById(memberId)
			.orElseThrow(() -> new AdminApplicationException(UserApplicationErrorCode.MEMBER_NOT_FOUND));
	}
}
