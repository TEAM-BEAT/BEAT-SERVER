package com.beat.admin.user.application.query

import com.beat.admin.exception.AdminApplicationException
import com.beat.admin.user.application.result.AdminUserResults
import com.beat.admin.user.application.result.AdminUserResults.AdminUserResult
import com.beat.admin.user.exception.UserApplicationErrorCode
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminUserQueryService(
    private val memberRepository: MemberRepository,
    private val userRepository: UserRepository,
) {
    fun findAllUsers(memberId: Long): AdminUserResults {
        validateMemberExists(memberId)
        val users = userRepository.findAll().map { it.toUserResult() }
        return AdminUserResults(users)
    }

    private fun Users.toUserResult(): AdminUserResult = AdminUserResult(requireNotNull(getId()), role.roleName)

    private fun validateMemberExists(memberId: Long) {
        memberRepository.findById(memberId)
            .orElseThrow { AdminApplicationException(UserApplicationErrorCode.MEMBER_NOT_FOUND) }
    }
}