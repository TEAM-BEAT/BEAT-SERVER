package com.beat.admin.user.facade;

import org.springframework.stereotype.Service;

import com.beat.admin.user.application.dto.response.UserFindAllResponse;
import com.beat.admin.user.application.service.query.AdminUserQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserFacade {

	private final AdminUserQueryService adminUserQueryService;

	public UserFindAllResponse checkMemberAndFindAllUsers(Long memberId) {
		return adminUserQueryService.findAllUsers(memberId);
	}
}
