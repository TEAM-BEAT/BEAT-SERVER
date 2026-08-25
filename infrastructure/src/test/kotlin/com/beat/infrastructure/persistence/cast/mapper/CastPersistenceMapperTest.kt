package com.beat.infrastructure.persistence.cast.mapper

import com.beat.domain.performance.model.Cast
import com.beat.infrastructure.persistence.cast.entity.CastJpaEntity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CastPersistenceMapperTest :
    FunSpec({
        val mapper = CastPersistenceMapper()

        test("toDomain은 scalar performanceId를 포함해 JPA entity 필드를 보존한다") {
            val entity =
                CastJpaEntity.rehydrate(
                    11L,
                    "cast-name",
                    "cast-role",
                    "https://example.com/cast.png",
                    22L,
                )

            val cast = mapper.toDomain(entity)

            cast.id shouldBe 11L
            cast.castName shouldBe "cast-name"
            cast.castRole shouldBe "cast-role"
            cast.castPhoto shouldBe "https://example.com/cast.png"
        }

        test("toEntity는 신규 cast의 생성 id를 null로 유지한다") {
            val cast =
                Cast.create(
                    "new-cast",
                    "new-role",
                    "https://example.com/new-cast.png",
                )

            val entity = mapper.toEntity(cast, 44L)

            cast.id shouldBe null
            entity.id shouldBe null
            entity.castName shouldBe "new-cast"
            entity.castRole shouldBe "new-role"
            entity.castPhoto shouldBe "https://example.com/new-cast.png"
            entity.performanceId shouldBe 44L
        }

        test("toEntity는 재구성된 domain 필드를 보존한다") {
            val cast =
                Cast.rehydrate(
                    31L,
                    "existing-cast",
                    "existing-role",
                    "https://example.com/existing-cast.png",
                )

            val entity = mapper.toEntity(cast, 41L)

            entity.id shouldBe 31L
            entity.castName shouldBe "existing-cast"
            entity.castRole shouldBe "existing-role"
            entity.castPhoto shouldBe "https://example.com/existing-cast.png"
            entity.performanceId shouldBe 41L
        }
    })
