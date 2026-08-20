package com.beat.admin.user.api

import com.beat.admin.user.api.response.UserFindAllResponse
import com.beat.admin.user.api.response.UserSuccessCode
import com.beat.admin.user.facade.AdminUserFacade
import com.beat.support.security.CurrentMember
import com.beat.global.support.response.SuccessResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class AdminUserController(
    private val adminUserFacade: AdminUserFacade,
) : AdminUserApi {

    @GetMapping("/users")
    override fun readAllUsers(
        @CurrentMember memberId: Long,
    ): ResponseEntity<SuccessResponse<UserFindAllResponse>> {
        val response = adminUserFacade.checkMemberAndFindAllUsers(memberId)
        return ResponseEntity.status(HttpStatus.OK)
            .body(SuccessResponse.of(UserSuccessCode.FETCH_ALL_USERS_SUCCESS, response))
    }
}
