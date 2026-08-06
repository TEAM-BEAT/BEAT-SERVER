package com.beat.admin.application

import com.beat.admin.user.application.query.AdminUserQueryService
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.user.model.Role
import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AdminUserQueryServiceTest {

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var adminUserQueryService: AdminUserQueryService

    @Test
    fun findAllUsersPreservesUserResponseShape() {
        `when`(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()))
        `when`(userRepository.findAll()).thenReturn(
            listOf(
                Users.rehydrate(1L, Role.USER),
                Users.rehydrate(2L, Role.ADMIN),
            ),
        )

        val response = adminUserQueryService.findAllUsers(MEMBER_ID)

        assertEquals(2, response.users.size)
        assertEquals(1L, response.users[0].id)
        assertEquals("ROLE_USER", response.users[0].role)
        assertEquals(2L, response.users[1].id)
        assertEquals("ROLE_ADMIN", response.users[1].role)
    }

    companion object {
        private const val MEMBER_ID = 7L

        private fun member(): Member =
            Member.rehydrate(MEMBER_ID, "admin", "admin@example.com", null, 1L, SocialIdentity.of(SocialType.KAKAO, 10L))
    }
}