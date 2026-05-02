package com.beat.apis.common.application;

public final class ApiEnumMapper {

	private ApiEnumMapper() {
	}

	public static <A extends Enum<A>, D extends Enum<D>> D toDomain(A apiEnum, Class<D> domainType) {
		if (apiEnum == null) {
			return null;
		}
		return Enum.valueOf(domainType, apiEnum.name());
	}

	public static <D extends Enum<D>, A extends Enum<A>> A fromDomain(D domainEnum, Class<A> apiType) {
		if (domainEnum == null) {
			return null;
		}
		return Enum.valueOf(apiType, domainEnum.name());
	}
}
