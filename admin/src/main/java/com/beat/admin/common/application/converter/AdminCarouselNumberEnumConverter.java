package com.beat.admin.common.application.converter;

import com.beat.admin.promotion.application.dto.request.AdminCarouselNumber;
import com.beat.domain.promotion.domain.CarouselNumber;

public final class AdminCarouselNumberEnumConverter {

	private AdminCarouselNumberEnumConverter() {
	}

	public static CarouselNumber toDomain(final AdminCarouselNumber carouselNumber) {
		if (carouselNumber == null) {
			return null;
		}

		return switch (carouselNumber) {
			case ONE -> CarouselNumber.ONE;
			case TWO -> CarouselNumber.TWO;
			case THREE -> CarouselNumber.THREE;
			case FOUR -> CarouselNumber.FOUR;
			case FIVE -> CarouselNumber.FIVE;
			case SIX -> CarouselNumber.SIX;
			case SEVEN -> CarouselNumber.SEVEN;
		};
	}

	public static AdminCarouselNumber toApi(final CarouselNumber carouselNumber) {
		if (carouselNumber == null) {
			return null;
		}

		return switch (carouselNumber) {
			case ONE -> AdminCarouselNumber.ONE;
			case TWO -> AdminCarouselNumber.TWO;
			case THREE -> AdminCarouselNumber.THREE;
			case FOUR -> AdminCarouselNumber.FOUR;
			case FIVE -> AdminCarouselNumber.FIVE;
			case SIX -> AdminCarouselNumber.SIX;
			case SEVEN -> AdminCarouselNumber.SEVEN;
		};
	}

	public static String toApiName(final CarouselNumber carouselNumber) {
		AdminCarouselNumber adminCarouselNumber = toApi(carouselNumber);
		return adminCarouselNumber == null ? null : adminCarouselNumber.name();
	}
}
