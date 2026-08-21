package com.beat.infra.persistence.promotion.entity

import com.beat.domain.promotion.model.CarouselNumber
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

class PromotionJpaEntityContractTest {

    @Test
    fun kotlinJpaPluginKeepsEntityOpenAndInstantiableForJpa() {
        val entityClass = PromotionJpaEntity::class.java
        val noArgConstructor = entityClass.getDeclaredConstructor()

        assertAll(
            {
                assertFalse(
                    Modifier.isFinal(entityClass.modifiers),
                    "kotlin-jpa/all-open must make @Entity classes non-final for JPA proxies",
                )
            },
            { assertEquals(0, noArgConstructor.parameterCount) },
            {
                assertTrue(
                    Modifier.isPublic(noArgConstructor.modifiers),
                    "kotlin-jpa/no-arg must emit a reflection-visible no-arg constructor",
                )
            },
            // Kotlin documents the no-arg artifact as synthetic; current 2.3.21 bytecode exposes it as
            // hidden/deprecated, so either marker proves this constructor came from the compiler plugin.
            {
                assertTrue(
                    noArgConstructor.isSynthetic ||
                        noArgConstructor.isAnnotationPresent(java.lang.Deprecated::class.java),
                    "kotlin-jpa/no-arg constructor should be compiler-generated rather than source-authored",
                )
            },
        )
    }

    @Test
    fun javaVisibleAccessorsAndProtectedSettersStayCompatibleWithMapperAndJpa() {
        val entityClass = PromotionJpaEntity::class.java

        assertAll(
            {
                assertEquals(
                    PromotionJpaEntity::class.java,
                    entityClass.getDeclaredMethod(
                        "rehydrate",
                        Long::class.javaObjectType,
                        String::class.java,
                        Long::class.javaObjectType,
                        String::class.java,
                        Boolean::class.javaPrimitiveType,
                        CarouselNumber::class.java,
                    ).returnType,
                )
            },
            { assertEquals(Long::class.javaObjectType, entityClass.getDeclaredMethod("getId").returnType) },
            { assertEquals(String::class.java, entityClass.getDeclaredMethod("getPromotionPhoto").returnType) },
            { assertEquals(Long::class.javaObjectType, entityClass.getDeclaredMethod("getPerformanceId").returnType) },
            { assertEquals(String::class.java, entityClass.getDeclaredMethod("getRedirectUrl").returnType) },
            { assertEquals(Boolean::class.javaPrimitiveType, entityClass.getDeclaredMethod("isExternal").returnType) },
            { assertEquals(CarouselNumber::class.java, entityClass.getDeclaredMethod("getCarouselNumber").returnType) },
            { assertProtectedSetter("setId", Long::class.javaObjectType) },
            { assertProtectedSetter("setPromotionPhoto", String::class.java) },
            { assertProtectedSetter("setPerformanceId", Long::class.javaObjectType) },
            { assertProtectedSetter("setRedirectUrl", String::class.java) },
            { assertProtectedSetter("setExternal", Boolean::class.javaPrimitiveType!!) },
            { assertProtectedSetter("setCarouselNumber", CarouselNumber::class.java) },
        )
    }

    private fun assertProtectedSetter(name: String, parameterType: Class<*>) {
        val method = PromotionJpaEntity::class.java.getDeclaredMethod(name, parameterType)
        assertTrue(Modifier.isProtected(method.modifiers)) {
            "$name${method.parameterTypes.contentToString()} must stay protected"
        }
    }
}
