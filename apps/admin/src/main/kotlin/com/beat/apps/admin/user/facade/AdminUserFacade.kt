package com.beat.apps.admin.user.facade

import com.beat.apps.admin.user.api.response.UserFindAllResponse
import com.beat.application.admin.user.query.AdminUserQueryService
import org.springframework.stereotype.Service

@Service
class AdminUserFacade(
    private val adminUserQueryService: AdminUserQueryService,
) {
    fun checkMemberAndFindAllUsers(memberId: Long): UserFindAllResponse =
        UserFindAllResponse(adminUserQueryService.findAllUsers(memberId))
}
