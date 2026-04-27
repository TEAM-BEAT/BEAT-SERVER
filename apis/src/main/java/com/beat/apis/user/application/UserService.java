package com.beat.apis.user.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.domain.user.domain.Users;
import com.beat.domain.user.exception.UserErrorCode;
import com.beat.domain.user.port.in.UserUseCase;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserUseCase {
	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public List<Users> findAllUsers() {
		return userRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public Users findUserByUserId(final Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));
	}
}
