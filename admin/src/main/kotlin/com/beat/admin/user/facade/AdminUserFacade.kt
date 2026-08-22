package com.beat.admin.user.facade

import com.beat.admin.user.api.response.UserFindAllResponse
import com.beat.application.admin.user.query.AdminUserQueryService
import org.springframework.stereotype.Service

@Service
class AdminUserFacade(
    private val adminUserQueryService: AdminUserQueryService,
) {
    fun checkMemberAndFindAllUsers(memberId: Long): UserFindAllResponse =
        UserFindAllResponse(adminUserQueryService.findAllUsers(memberId))
}
