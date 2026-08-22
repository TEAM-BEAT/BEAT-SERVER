package com.beat.application.admin.user.query

import com.beat.application.admin.exception.AdminApplicationException
import com.beat.application.admin.exception.translateDomainFailure
import com.beat.application.admin.user.exception.UserApplicationErrorCode
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
        return translateDomainFailure {
            validateMemberExists(memberId)
            val users = userRepository.findAll().map { it.toUserResult() }
            AdminUserResults(users)
        }
    }

    private fun Users.toUserResult(): AdminUserResults.AdminUserResult =
        AdminUserResults.AdminUserResult(requireNotNull(id), role.roleName)

    private fun validateMemberExists(memberId: Long) {
        memberRepository.findById(memberId)
            ?: throw AdminApplicationException(UserApplicationErrorCode.MEMBER_NOT_FOUND)
    }
}
