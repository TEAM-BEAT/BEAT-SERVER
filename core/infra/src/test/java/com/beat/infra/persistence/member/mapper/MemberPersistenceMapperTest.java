package com.beat.infra.persistence.member.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.beat.domain.member.model.Member;
import com.beat.domain.member.model.SocialType;
import com.beat.domain.member.vo.SocialIdentity;
import com.beat.infra.persistence.exception.PersistenceMappingException;
import com.beat.infra.persistence.member.entity.MemberJpaEntity;

class MemberPersistenceMapperTest {

	private final MemberPersistenceMapper mapper = new MemberPersistenceMapper();

	@Test
	void toDomainPreservesSocialIdentityAndScalarUserId() {
		LocalDateTime deletedAt = LocalDateTime.of(2026, 7, 16, 12, 30);
		MemberJpaEntity entity = MemberJpaEntity.rehydrate(
			11L,
			"nickname",
			"member@example.com",
			deletedAt,
			22L,
			33L,
			SocialType.KAKAO
		);

		Member member = mapper.toDomain(entity);

		assertAll(
			() -> assertEquals(11L, member.getId()),
			() -> assertEquals("nickname", member.getNickname()),
			() -> assertEquals("member@example.com", member.getEmail()),
			() -> assertEquals(deletedAt, member.getDeletedAt()),
			() -> assertEquals(22L, member.getUserId()),
			() -> assertEquals(SocialIdentity.of(SocialType.KAKAO, 33L), member.getSocialIdentity())
		);
	}

	@Test
	void roundTripPreservesNewMemberAndGeneratedIdRemainsNull() {
		SocialIdentity socialIdentity = SocialIdentity.of(SocialType.KAKAO, 44L);
		Member member = Member.create("new-member", null, 55L, socialIdentity);

		Member roundTrip = mapper.toDomain(mapper.toEntity(member));

		assertAll(
			() -> assertNull(roundTrip.getId()),
			() -> assertEquals("new-member", roundTrip.getNickname()),
			() -> assertNull(roundTrip.getEmail()),
			() -> assertNull(roundTrip.getDeletedAt()),
			() -> assertEquals(55L, roundTrip.getUserId()),
			() -> assertEquals(socialIdentity, roundTrip.getSocialIdentity())
		);
	}

	@Test
	void invalidStoredSocialIdentityIsTranslatedToPersistenceFailure() {
		MemberJpaEntity corrupted = mock(MemberJpaEntity.class);

		assertThrows(PersistenceMappingException.class, () -> mapper.toDomain(corrupted));
	}
}
