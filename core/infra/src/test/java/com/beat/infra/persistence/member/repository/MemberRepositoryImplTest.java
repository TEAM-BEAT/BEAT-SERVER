package com.beat.infra.persistence.member.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MemberRepositoryImplTest {

	@ParameterizedTest
	@CsvSource({
		"uk_member_social_identity, uk_member_social_identity",
		"member.uk_member_social_identity, uk_member_social_identity",
		"beatDev.member.uk_member_social_identity, uk_member_social_identity",
		"'`member`.`uk_member_social_identity`', uk_member_social_identity",
		"'\"member\".\"uk_member_social_identity\"', uk_member_social_identity",
		"'''member.uk_member_social_identity''', uk_member_social_identity"
	})
	void normalizesQualifiedAndQuotedConstraintName(String constraintName, String expected) {
		assertEquals(expected, MemberRepositoryImpl.normalizeConstraintName(constraintName));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "`", "''", "member."})
	void normalizesEmptyIdentifierToNull(String constraintName) {
		assertNull(MemberRepositoryImpl.normalizeConstraintName(constraintName));
	}

	@Test
	void handlesNullConstraintName() {
		assertNull(MemberRepositoryImpl.normalizeConstraintName(null));
	}
}
