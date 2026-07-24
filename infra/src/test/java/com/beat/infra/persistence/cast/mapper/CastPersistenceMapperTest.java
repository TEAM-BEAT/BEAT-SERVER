package com.beat.infra.persistence.cast.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.beat.domain.performance.model.Cast;
import com.beat.infra.persistence.cast.entity.CastJpaEntity;

class CastPersistenceMapperTest {

	private final CastPersistenceMapper mapper = new CastPersistenceMapper();

	@Test
	void toDomainPreservesJpaEntityFieldsIncludingScalarPerformanceId() {
		CastJpaEntity entity = CastJpaEntity.rehydrate(
			11L,
			"cast-name",
			"cast-role",
			"https://example.com/cast.png",
			22L
		);

		Cast cast = mapper.toDomain(entity);

		assertAll(
			() -> assertEquals(11L, cast.getId()),
			() -> assertEquals("cast-name", cast.getCastName()),
			() -> assertEquals("cast-role", cast.getCastRole()),
			() -> assertEquals("https://example.com/cast.png", cast.getCastPhoto())
		);
	}

	@Test
	void toEntityKeepsGeneratedIdNullForNewCast() {
		Cast cast = Cast.create(
			"new-cast",
			"new-role",
			"https://example.com/new-cast.png"
		);

		CastJpaEntity entity = mapper.toEntity(cast, 44L);

		assertAll(
			() -> assertNull(cast.getId()),
			() -> assertNull(entity.getId()),
			() -> assertEquals("new-cast", entity.getCastName()),
			() -> assertEquals("new-role", entity.getCastRole()),
			() -> assertEquals("https://example.com/new-cast.png", entity.getCastPhoto()),
			() -> assertEquals(44L, entity.getPerformanceId())
		);
	}

	@Test
	void toEntityPreservesRehydratedDomainFields() {
		Cast cast = Cast.rehydrate(
			31L,
			"existing-cast",
			"existing-role",
			"https://example.com/existing-cast.png"
		);

		CastJpaEntity entity = mapper.toEntity(cast, 41L);

		assertAll(
			() -> assertEquals(31L, entity.getId()),
			() -> assertEquals("existing-cast", entity.getCastName()),
			() -> assertEquals("existing-role", entity.getCastRole()),
			() -> assertEquals("https://example.com/existing-cast.png", entity.getCastPhoto()),
			() -> assertEquals(41L, entity.getPerformanceId())
		);
	}
}
