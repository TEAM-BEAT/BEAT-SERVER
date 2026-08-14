package com.beat.domain.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.beat.domain.performance.vo.RunningTime;
import com.beat.domain.user.model.Role;
import com.beat.domain.user.model.Users;

class DomainEntityEqualityTest {

	@Test
	void persistedEntitiesUseIdentityInsteadOfStateForEquality() {
		Users user = Users.rehydrate(1L, Role.USER);
		Users sameIdentityWithDifferentState = Users.rehydrate(1L, Role.ADMIN);
		Users differentIdentity = Users.rehydrate(2L, Role.USER);

		assertEquals(user, sameIdentityWithDifferentState);
		assertEquals(user.hashCode(), sameIdentityWithDifferentState.hashCode());
		assertNotEquals(user, differentIdentity);
	}

	@Test
	void transientEntitiesOnlyEqualTheSameInstance() {
		Users user = Users.create();

		assertEquals(user, user);
		assertNotEquals(user, Users.create());
	}

	@Test
	void valueObjectsKeepStructuralEquality() {
		assertEquals(RunningTime.of(90), RunningTime.of(90));
		assertNotEquals(RunningTime.of(90), RunningTime.of(100));
	}
}
