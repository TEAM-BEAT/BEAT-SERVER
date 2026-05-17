package com.beat.infra.persistence.promotion.entity;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.beat.domain.promotion.domain.CarouselNumber;

class PromotionJpaEntityContractTest {

	@Test
	void kotlinJpaPluginKeepsEntityOpenAndInstantiableForJpa() throws Exception {
		Class<PromotionJpaEntity> entityClass = PromotionJpaEntity.class;
		Constructor<PromotionJpaEntity> noArgConstructor = entityClass.getDeclaredConstructor();

		assertAll(
			() -> assertFalse(Modifier.isFinal(entityClass.getModifiers()),
				"kotlin-jpa/all-open must make @Entity classes non-final for JPA proxies"),
			() -> assertEquals(0, noArgConstructor.getParameterCount()),
			() -> assertTrue(Modifier.isPublic(noArgConstructor.getModifiers()),
				"kotlin-jpa/no-arg must emit a reflection-visible no-arg constructor"),
			// Kotlin documents the no-arg artifact as synthetic; current 2.3.20 bytecode exposes it as
			// hidden/deprecated, so either marker proves this constructor came from the compiler plugin.
			() -> assertTrue(noArgConstructor.isSynthetic() || noArgConstructor.isAnnotationPresent(Deprecated.class),
				"kotlin-jpa/no-arg constructor should be compiler-generated rather than source-authored")
		);
	}

	@Test
	void javaVisibleAccessorsAndProtectedSettersStayCompatibleWithMapperAndJpa() throws Exception {
		Class<PromotionJpaEntity> entityClass = PromotionJpaEntity.class;

		assertAll(
			() -> assertEquals(PromotionJpaEntity.class, entityClass
				.getDeclaredMethod("rehydrate", Long.class, String.class, Long.class, String.class, boolean.class,
					CarouselNumber.class)
				.getReturnType()),
			() -> assertEquals(Long.class, entityClass.getDeclaredMethod("getId").getReturnType()),
			() -> assertEquals(String.class, entityClass.getDeclaredMethod("getPromotionPhoto").getReturnType()),
			() -> assertEquals(Long.class, entityClass.getDeclaredMethod("getPerformanceId").getReturnType()),
			() -> assertEquals(String.class, entityClass.getDeclaredMethod("getRedirectUrl").getReturnType()),
			() -> assertEquals(boolean.class, entityClass.getDeclaredMethod("isExternal").getReturnType()),
			() -> assertEquals(CarouselNumber.class, entityClass.getDeclaredMethod("getCarouselNumber").getReturnType()),
			() -> assertProtectedSetter("setId", Long.class),
			() -> assertProtectedSetter("setPromotionPhoto", String.class),
			() -> assertProtectedSetter("setPerformanceId", Long.class),
			() -> assertProtectedSetter("setRedirectUrl", String.class),
			() -> assertProtectedSetter("setExternal", boolean.class),
			() -> assertProtectedSetter("setCarouselNumber", CarouselNumber.class)
		);
	}

	private void assertProtectedSetter(String name, Class<?> parameterType) throws Exception {
		Method method = PromotionJpaEntity.class.getDeclaredMethod(name, parameterType);
		assertTrue(Modifier.isProtected(method.getModifiers()),
			() -> name + Arrays.toString(method.getParameterTypes()) + " must stay protected");
	}
}
