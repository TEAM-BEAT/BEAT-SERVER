package com.beat.application.admin.user.query

import com.beat.application.admin.exception.AdminApplicationException
import com.beat.application.admin.user.exception.UserApplicationErrorCode
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.user.model.Role
import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class AdminUserQueryApplicationSpec : FunSpec({

    context("관리자 회원이 존재하면") {
        test("모든 User의 id와 role을 조회 결과로 매핑한다") {
            val userRepository = RecordingUserRepository(
                listOf(
                    Users.rehydrate(1L, Role.USER),
                    Users.rehydrate(2L, Role.ADMIN),
                ),
            )
            val service = AdminUserQueryService(
                memberRepository = StubMemberRepository(member()),
                userRepository = userRepository,
            )

            val result = service.findAllUsers(MEMBER_ID)

            result.users.shouldContainExactly(
                AdminUserResults.AdminUserResult(1L, "ROLE_USER"),
                AdminUserResults.AdminUserResult(2L, "ROLE_ADMIN"),
            )
            userRepository.findAllCalls shouldBe 1
        }
    }

    context("관리자 회원이 존재하지 않으면") {
        test("회원 없음 failure를 반환하고 User 목록을 조회하지 않는다") {
            val userRepository = RecordingUserRepository(emptyList())
            val service = AdminUserQueryService(
                memberRepository = StubMemberRepository(null),
                userRepository = userRepository,
            )

            val exception = shouldThrow<AdminApplicationException> {
                service.findAllUsers(MEMBER_ID)
            }

            exception.errorCode shouldBe UserApplicationErrorCode.MEMBER_NOT_FOUND
            userRepository.findAllCalls shouldBe 0
        }
    }
})

private class StubMemberRepository(
    private val member: Member?,
) : MemberRepository {
    override fun findById(id: Long): Member? =
        member?.takeIf { it.id == id }

    override fun save(member: Member): Member = member

    override fun findBySocialIdentity(socialIdentity: SocialIdentity): Member? = null

    override fun count(): Long = if (member == null) 0 else 1
}

private class RecordingUserRepository(
    private val users: List<Users>,
) : UserRepository {
    var findAllCalls: Int = 0
        private set

    override fun findById(id: Long): Users? =
        users.firstOrNull { it.id == id }

    override fun findAll(): List<Users> {
        findAllCalls += 1
        return users
    }

    override fun save(users: Users): Users = users

    override fun delete(users: Users) = Unit
}

private const val MEMBER_ID = 7L

private fun member(): Member = Member.rehydrate(
    id = MEMBER_ID,
    nickname = "admin",
    email = "admin@example.com",
    deletedAt = null,
    userId = 2L,
    socialIdentity = SocialIdentity.of(SocialType.KAKAO, 10L),
)
