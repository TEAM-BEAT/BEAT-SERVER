package com.beat.apis.user.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.user.application.exception.UserApplicationErrorCode;
import com.beat.apis.user.application.result.UserAuthenticationResult;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public UserAuthenticationResult findUserAuthenticationByUserId(final Long userId) {
		return userRepository.findById(userId)
			.map(this::toAuthenticationResult)
			.orElseThrow(() -> new NotFoundException(UserApplicationErrorCode.USER_NOT_FOUND));
	}

	private UserAuthenticationResult toAuthenticationResult(Users user) {
		return UserAuthenticationResult.of(user.getId(), user.getRole().getRoleName());
	}
}
