package com.beat.infra.persistence.user.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.beat.domain.user.model.Role;
import com.beat.domain.user.model.Users;
import com.beat.infra.persistence.user.entity.UsersJpaEntity;

class UsersPersistenceMapperTest {

	private final UsersPersistenceMapper mapper = new UsersPersistenceMapper();

	@Test
	void roundTripPreservesPersistedUserRole() {
		Users users = Users.rehydrate(11L, Role.ADMIN);

		Users roundTrip = mapper.toDomain(mapper.toEntity(users));

		assertAll(
			() -> assertEquals(11L, roundTrip.getId()),
			() -> assertEquals(Role.ADMIN, roundTrip.getRole())
		);
	}

	@Test
	void toEntityKeepsGeneratedIdNullForNewUser() {
		Users users = Users.createWithRole(Role.MEMBER);

		UsersJpaEntity entity = mapper.toEntity(users);

		assertAll(
			() -> assertNull(entity.getId()),
			() -> assertEquals(Role.MEMBER, entity.getRole())
		);
	}
}
