package com.beat.domain.admin.api;

import com.beat.domain.admin.application.AdminService;
import com.beat.domain.admin.exception.AdminSuccessCode;
import com.beat.domain.admin.application.dto.UserFindAllResponse;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.SuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<SuccessResponse<UserFindAllResponse>> readAllUsers(
            @CurrentMember Long memberId) {
        UserFindAllResponse response = adminService.findAllUsers(memberId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.of(AdminSuccessCode.FETCH_ALL_USERS_SUCCESS, response));
    }
}