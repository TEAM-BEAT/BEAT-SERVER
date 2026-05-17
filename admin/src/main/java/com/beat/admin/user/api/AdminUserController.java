package com.beat.admin.user.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beat.admin.api.response.AdminSuccessCode;
import com.beat.admin.user.application.dto.response.UserFindAllResponse;
import com.beat.admin.user.facade.AdminUserFacade;
import com.beat.gateway.security.servlet.CurrentMember;
import com.beat.global.support.response.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController implements AdminUserApi {

	private final AdminUserFacade adminUserFacade;

	@Override
	@GetMapping("/users")
	public ResponseEntity<SuccessResponse<UserFindAllResponse>> readAllUsers(@CurrentMember Long memberId) {
		UserFindAllResponse response = adminUserFacade.checkMemberAndFindAllUsers(memberId);
		return ResponseEntity.status(HttpStatus.OK)
			.body(SuccessResponse.of(AdminSuccessCode.FETCH_ALL_USERS_SUCCESS, response));
	}
}
