package com.beat.infra.persistence.promotion.entity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.lang.reflect.Modifier

class PromotionJpaEntityContractTest : FunSpec({

    test("kotlinJpaPluginKeepsEntityOpenAndInstantiableForJpa") {
        val entityClass = PromotionJpaEntity::class.java
        val noArgConstructor = entityClass.getDeclaredConstructor()

        Modifier.isFinal(entityClass.modifiers) shouldBe false
        noArgConstructor.parameterCount shouldBe 0
        Modifier.isPublic(noArgConstructor.modifiers) shouldBe true
        // Kotlin documents the no-arg artifact as synthetic; current 2.3.21 bytecode exposes it as
        // hidden/deprecated, so either marker proves this constructor came from the compiler plugin.
        (noArgConstructor.isSynthetic ||
            noArgConstructor.isAnnotationPresent(java.lang.Deprecated::class.java)) shouldBe true
    }
})
