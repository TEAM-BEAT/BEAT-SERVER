package com.beat.admin.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.admin.user.application.result.AdminUserResults;
import com.beat.admin.user.application.query.AdminUserQueryService;
import com.beat.domain.member.model.Member;
import com.beat.domain.member.vo.SocialIdentity;
import com.beat.domain.member.model.SocialType;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.user.model.Role;
import com.beat.domain.user.model.Users;
import com.beat.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserQueryServiceTest {

	private static final long MEMBER_ID = 7L;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private AdminUserQueryService adminUserQueryService;

	@Test
	void findAllUsersPreservesUserResponseShape() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		when(userRepository.findAll()).thenReturn(List.of(
			Users.rehydrate(1L, Role.USER),
			Users.rehydrate(2L, Role.ADMIN)
		));

		AdminUserResults response = adminUserQueryService.findAllUsers(MEMBER_ID);

		assertEquals(2, response.users().size());
		assertEquals(1L, response.users().get(0).id());
		assertEquals("ROLE_USER", response.users().get(0).role());
		assertEquals(2L, response.users().get(1).id());
		assertEquals("ROLE_ADMIN", response.users().get(1).role());
	}

	private static Member member() {
		return Member.rehydrate(MEMBER_ID, "admin", "admin@example.com", null, 1L, SocialIdentity.of(SocialType.KAKAO, 10L));
	}
}
