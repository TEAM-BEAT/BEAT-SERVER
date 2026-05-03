package com.beat.apis.common.application;

public final class ApiEnumMapper {

	private ApiEnumMapper() {
	}

	public static <A extends Enum<A>, D extends Enum<D>> D toDomain(A apiEnum, Class<D> domainType) {
		if (apiEnum == null) {
			return null;
		}
		return mapByName(apiEnum, domainType);
	}

	public static <D extends Enum<D>, A extends Enum<A>> A fromDomain(D domainEnum, Class<A> apiType) {
		if (domainEnum == null) {
			return null;
		}
		return mapByName(domainEnum, apiType);
	}

	private static <S extends Enum<S>, T extends Enum<T>> T mapByName(S sourceEnum, Class<T> targetType) {
		try {
			return Enum.valueOf(targetType, sourceEnum.name());
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException(
				"Failed to map enum value '%s' from %s to %s"
					.formatted(sourceEnum.name(), sourceEnum.getDeclaringClass().getName(), targetType.getName()),
				exception
			);
		}
	}
}
